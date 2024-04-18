package src.optimize.SCCP;

import src.IR.instruction.*;
import src.IR.statement.FuncDef;
import src.Util.cell.Cell;
import src.optimize.SCCP.SCCPNode.EdgeNode;
import src.optimize.SCCP.SCCPNode.SCCPNode;
import src.optimize.SCCP.SCCPNode.SSANode;

import java.util.*;

public class FunctionSCCP {
    public Queue<SCCPNode> workList;
    public HashMap<String, Block> blocks;//块名->块
    public HashSet<String> edgeVisited;// from块名+'#'+to块名
    public HashSet<String> blockVisited;
    public HashMap<String, List<Instruction>> SSAInstr;//变量名->use
    public HashSet<Instruction> SSAPutAlready;
    public HashMap<String, LatticeCell> SSALattice;
    public FuncDef funcDef;
    private List<Instruction> newInstrList;
    private int cnt = 1;

    public FunctionSCCP(FuncDef funcDef_) {
        workList = new ArrayDeque<>();
        blocks = new HashMap<>();
        edgeVisited = new HashSet<>();
        blockVisited = new HashSet<>();
        SSAInstr = new HashMap<>();
        SSAPutAlready = new HashSet<>();
        SSALattice = new HashMap<>();
        funcDef = funcDef_;
    }

    public void optimize() {
        setInfo();
        sccp();
        recollect();
    }

    private void setInfo() {
        // 初始化参数
        int size = funcDef.parameterTypeList.size();
        if (size > 0) {
            if (funcDef.isClassMethod) {
                findLattice("%this").status = LatticeCell.UNCERTAIN_NEW;
            } else {
                findLattice("%_0").status = LatticeCell.UNCERTAIN_NEW;
            }
        }
        for (int i = 1; i < size; ++i) {
            if (funcDef.isClassMethod) {
                findLattice("%_" + (i - 1)).status = LatticeCell.UNCERTAIN_NEW;
            } else {
                findLattice("%_" + i).status = LatticeCell.UNCERTAIN_NEW;
            }
        }
        funcDef.irList.forEach(block -> blocks.put(block.label, block));
    }

    private void putSSA(String varName, Instruction instr) {
        if (varName == null || SSAPutAlready.contains(instr)) {
            return;
        }
        SSAInstr.computeIfAbsent(varName, k -> new ArrayList<>()).add(instr);
    }

    private void putSSA(Cell cell, Instruction instr) {
        if (cell == null || cell.isConst) {
            return;
        }
        putSSA(cell.varName, instr);
    }

    private LatticeCell findLattice(String varName) {
        if (varName == null) {
            return null;
        }
        LatticeCell tmp = SSALattice.get(varName);
        if (tmp == null) {
            SSALattice.put(varName, tmp = new LatticeCell(varName));
        }
        return tmp;
    }

    private LatticeCell findLattice(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.isConst) {
            return new LatticeCell(cell.varValue);
        } else {
            return findLattice(cell.varName);
        }
    }

    private void sccp() {
        workList.add(new EdgeNode(null, "entry"));
        while (!workList.isEmpty()) {
            SCCPNode now = workList.poll();
            if (now instanceof EdgeNode) {
                String mark = ((EdgeNode) now).fromBlock + '#' + ((EdgeNode) now).toBlock;
                if (!edgeVisited.contains(mark)) {
                    edgeVisited.add(mark);
                    Block block = blocks.get(((EdgeNode) now).toBlock);
                    boolean toVisitBlock = !blockVisited.contains(((EdgeNode) now).toBlock);
                    blockVisited.add(((EdgeNode) now).toBlock);
                    for (Instruction instr : block.instrList) {
                        if (instr instanceof Phi) {
                            visitPhi((Phi) instr);
                        } else if (toVisitBlock) {
                            if (instr instanceof Br) {
                                visitBr((Br) instr);
                                break;
                            }
                            if (instr instanceof Ret) {
                                visitInstr(instr);
                                break;
                            } else {
                                visitInstr(instr);
                            }
                        } else {
                            break;
                        }
                    }
                }
            } else if (now instanceof SSANode) {
                var useList = SSAInstr.get(((SSANode) now).varName);
                if (useList != null) {
                    for (var instr : useList) {
                        if (instr instanceof Phi) {
                            visitPhi((Phi) instr);
                        } else if (instr instanceof Br) {
                            visitBr((Br) instr);
                        } else {
                            visitInstr(instr);
                        }
                    }
                }
            }
        }
    }

    private void visitPhi(Phi phi) {
        for (var assign : phi.assignBlockList) {
            putSSA(assign.variable, phi);
        }
        SSAPutAlready.add(phi);
        var lattice = findLattice(phi.result);
        boolean flag = false;
        for (var assign : phi.assignBlockList) {
            if (blockVisited.contains(assign.label.substring(1))) {
                if (lattice.update(findLattice(assign.variable))) {
                    flag = true;
                }
            }
        }
        if (flag) {
            workList.add(new SSANode(phi.result));
        }
    }

    private void visitInstr(Instruction instr) {
        if (instr instanceof Binary) {
            putSSA(((Binary) instr).left, instr);
            putSSA(((Binary) instr).right, instr);
            var lattice = findLattice(((Binary) instr).output);
            var lattice_lhs = findLattice(((Binary) instr).left);
            var lattice_rhs = findLattice(((Binary) instr).right);
            if (lattice.update(lattice_lhs, lattice_rhs, ((Binary) instr).op)) {
                workList.add(new SSANode(((Binary) instr).output));
            }
        } else if (instr instanceof Icmp) {
            putSSA(((Icmp) instr).left, instr);
            putSSA(((Icmp) instr).right, instr);
            var lattice = findLattice(((Icmp) instr).output);
            var lattice_lhs = findLattice(((Icmp) instr).left);
            var lattice_rhs = findLattice(((Icmp) instr).right);
            if (lattice.update(lattice_lhs, lattice_rhs, ((Icmp) instr).cond)) {
                workList.add(new SSANode(((Icmp) instr).output));
            }
        } else if (instr instanceof Load) {
            putSSA(((Load) instr).fromPointer, instr);
            findLattice(((Load) instr).toVarName).status = LatticeCell.UNCERTAIN_NEW;
        } else if (instr instanceof Store) {
            putSSA(((Store) instr).value, instr);
            putSSA(((Store) instr).toPointer, instr);
        } else if (instr instanceof Call) {
            for (var para : ((Call) instr).callList) {
                putSSA(para.varName, instr);
            }
            if (((Call) instr).resultVar != null) {
                findLattice(((Call) instr).resultVar).status = LatticeCell.UNCERTAIN_NEW;
            }
        } else if (instr instanceof Getelementptr) {
            putSSA(((Getelementptr) instr).from, instr);
            putSSA(((Getelementptr) instr).index, instr);
            findLattice(((Getelementptr) instr).result).status = LatticeCell.UNCERTAIN_NEW;
        } else if (instr instanceof Ret) {
            if (((Ret) instr).retVar != null) {
                putSSA(((Ret) instr).retVar, instr);
            }
        }
        SSAPutAlready.add(instr);
    }

    private void visitBr(Br br) {
        putSSA(br.condition, br);
        SSAPutAlready.add(br);
        if (br.condition == null) {
            workList.add(new EdgeNode(br.nowLabel.substring(1), br.trueLabel.substring(1)));
        } else {
            var lattice = findLattice(br.condition);
            if (lattice.status == LatticeCell.CONST) {
                if (lattice.constValue == 0) {
                    workList.add(new EdgeNode(br.nowLabel.substring(1), br.falseLabel.substring(1)));
                } else {
                    workList.add(new EdgeNode(br.nowLabel.substring(1), br.trueLabel.substring(1)));
                }
            } else {
                workList.add(new EdgeNode(br.nowLabel.substring(1), br.falseLabel.substring(1)));
                workList.add(new EdgeNode(br.nowLabel.substring(1), br.trueLabel.substring(1)));
            }
        }
    }

    private void recollect() {
        List<Block> newIrList = new ArrayList<>();
        for (Block block : funcDef.irList) {
            if (blockVisited.contains(block.label)) {
                newIrList.add(block);
            } else {
                continue;
            }
            newInstrList = new ArrayList<>();
            for (Instruction instr : block.instrList) {
                if (instr instanceof Phi) {
                    if (!remove(((Phi) instr).result)) {
                        for (var assign : ((Phi) instr).assignBlockList) {
                            if (!assign.variable.isConst) {
                                setByLattice(assign.variable, findLattice(assign.variable));
                            }
                        }
                        newInstrList.add(instr);
                    }
                } else if (instr instanceof Binary) {
                    if (!remove(((Binary) instr).output)) {
                        var lattice = findLattice(((Binary) instr).output);
                        if (lattice.status == LatticeCell.UNCERTAIN_COPY) {
                            setByLattice((Binary) instr, lattice);
                        } else {
                            if (!((Binary) instr).left.isConst) {
                                setByLattice(((Binary) instr).left, findLattice(((Binary) instr).left));
                            }
                            if (!((Binary) instr).right.isConst) {
                                setByLattice(((Binary) instr).right, findLattice(((Binary) instr).right));
                            }
                            newInstrList.add(instr);
                        }
                    }
                } else if (instr instanceof Icmp) {
                    if (!remove(((Icmp) instr).output)) {
                        if (!((Icmp) instr).left.isConst) {
                            setByLattice(((Icmp) instr).left, findLattice(((Icmp) instr).left));
                        }
                        if (!((Icmp) instr).right.isConst) {
                            setByLattice(((Icmp) instr).right, findLattice(((Icmp) instr).right));
                        }
                        newInstrList.add(instr);
                    }
                } else if (instr instanceof Load) {
                    if (!remove(((Load) instr).toVarName)) {
                        ((Load) instr).fromPointer = setByLattice(((Load) instr).fromPointer);
                        newInstrList.add(instr);
                    }
                } else if (instr instanceof Store) {
                    if (!((Store) instr).value.isConst) {
                        setByLattice(((Store) instr).value, findLattice(((Store) instr).value));
                        ((Store) instr).toPointer = setByLattice(((Store) instr).toPointer);
                    }
                    newInstrList.add(instr);
                } else if (instr instanceof Call) {
                    for (var para : ((Call) instr).callList) {
                        if (!para.isConst) {
                            setByLattice(para, findLattice(para.varName));
                        }
                    }
                    newInstrList.add(instr);
                } else if (instr instanceof Getelementptr) {
                    if (!remove(((Getelementptr) instr).result)) {
                        if (!((Getelementptr) instr).index.isConst) {
                            setByLattice(((Getelementptr) instr).index, findLattice(((Getelementptr) instr).index));
                        }
                        ((Getelementptr) instr).from = setByLattice(((Getelementptr) instr).from);
                        newInstrList.add(instr);
                    }
                } else if (instr instanceof Ret) {
                    if (((Ret) instr).retVar != null && !((Ret) instr).retVar.isConst) {
                        setByLattice(((Ret) instr).retVar, findLattice(((Ret) instr).retVar));
                    }
                    newInstrList.add(instr);
                } else if (instr instanceof Br) {
                    var lattice = findLattice(((Br) instr).condition);
                    if (lattice != null && lattice.status == LatticeCell.CONST) {
                        if (lattice.constValue == 0) {
                            ((Br) instr).trueLabel = ((Br) instr).falseLabel;
                        }
                        ((Br) instr).condition = null;
                    }
                    newInstrList.add(instr);
                }
            }
            block.instrList = newInstrList;
        }
        funcDef.irList = newIrList;
    }

    private void setByLattice(Cell cell, LatticeCell latticeCell) {
        if (latticeCell.status == LatticeCell.CONST) {
            cell.set(latticeCell.constValue);
        } else if (latticeCell.status == LatticeCell.UNCERTAIN_COPY) {
            if (remove(cell.varName)) {
                if (latticeCell.imme_mul == -1) {
                    Binary binary = new Binary("-");
                    binary.left.set(latticeCell.imme_add);
                    binary.right.set(latticeCell.varCopy);
                    binary.output = latticeCell.varCopy + "-sccp" + cnt++;
                    cell.set(binary.output);
                    newInstrList.add(binary);
                    return;
                }
                String copy = latticeCell.varCopy;
                if (latticeCell.imme_mul != 1) {
                    Binary binary = new Binary("*");
                    binary.left.set(copy);
                    binary.right.set(latticeCell.imme_mul);
                    copy += "-sccp" + cnt++;
                    binary.output = copy;
                    newInstrList.add(binary);
                }
                if (latticeCell.imme_add != 0) {
                    Binary binary = new Binary("+");
                    binary.left.set(copy);
                    binary.right.set(latticeCell.imme_add);
                    copy += "-sccp" + cnt++;
                    binary.output = copy;
                    newInstrList.add(binary);
                }
                cell.set(copy);
            } else {
                cell.set(latticeCell.varNew);
            }
        } else {
            cell.set(latticeCell.varNew);
        }
    }

    private String setByLattice(String varName) {
        var lattice = findLattice(varName);
        if (lattice == null) {
            return varName;
        }
        if (lattice.status == LatticeCell.UNCERTAIN_COPY) {
            if (remove(varName)) {
                if (lattice.imme_mul == -1) {
                    Binary binary = new Binary("-");
                    binary.left.set(lattice.imme_add);
                    binary.right.set(lattice.varCopy);
                    binary.output = lattice.varCopy + "-sccp" + cnt++;
                    newInstrList.add(binary);
                    return binary.output;
                }
                String copy = lattice.varCopy;
                if (lattice.imme_mul != 1) {
                    Binary binary = new Binary("*");
                    binary.left.set(copy);
                    binary.right.set(lattice.constValue);
                    copy += "-sccp" + cnt++;
                    binary.output = copy;
                    newInstrList.add(binary);
                }
                if (lattice.imme_add != 0) {
                    Binary binary = new Binary("+");
                    binary.left.set(copy);
                    binary.right.set(lattice.imme_add);
                    copy += "-sccp" + cnt++;
                    binary.output = copy;
                    newInstrList.add(binary);
                }
                return copy;
            } else {
                return varName;
            }
        } else {
            return varName;
        }
    }

    private void setByLattice(Binary instr, LatticeCell latticeCell) {
        if ((instr.left.isConst && !remove(instr.right.varName)) ||
                (instr.right.isConst && !remove(instr.left.varName))) {
            newInstrList.add(instr);
            return;
        }
        if (latticeCell.imme_mul == -1) {
            instr.left.set(latticeCell.imme_add);
            instr.right.set(latticeCell.varCopy);
            instr.op = "sub";
            newInstrList.add(instr);
            return;
        }
        Binary binary = null;
        String copy = latticeCell.varCopy;
        if (latticeCell.imme_mul != 1) {
            binary = new Binary("*");
            binary.left.set(copy);
            binary.right.set(latticeCell.imme_mul);
            copy += "-sccp" + cnt++;
            binary.output = copy;
            newInstrList.add(binary);
        }
        if (latticeCell.imme_add != 0) {
            binary = new Binary("+");
            binary.left.set(copy);
            binary.right.set(latticeCell.imme_add);
            binary.output = instr.output;
            newInstrList.add(binary);
        } else {
            binary.output = instr.output;
        }
    }

    private boolean remove(String varName) {
        var lattice = findLattice(varName);
        if (lattice.status == LatticeCell.UNCERTAIN_NEW) {
            return false;
        } else if (lattice.status >= LatticeCell.CONST) {
            return true;
        } else {
            if (lattice.imme_add == 0 && lattice.imme_mul == 1) {
                return true;
            } else {
                var useList = SSAInstr.get(varName);
                return !(useList.size() >= 2 || (useList.size() == 1 && useList.get(0) instanceof Phi));
            }
        }
    }
}