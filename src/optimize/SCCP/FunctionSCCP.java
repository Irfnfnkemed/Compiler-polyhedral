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
            List<Instruction> newInstrList = new ArrayList<>();
            for (Instruction instr : block.instrList) {
                if (instr instanceof Phi) {
                    if (!findLattice(((Phi) instr).result).removed()) {
                        newInstrList.add(instr);
                        for (var assign : ((Phi) instr).assignBlockList) {
                            if (!assign.variable.isConst) {
                                setByLattice(assign.variable, findLattice(assign.variable));
                            }
                        }
                    }
                } else if (instr instanceof Binary) {
                    if (!findLattice(((Binary) instr).output).removed()) {
                        newInstrList.add(instr);
                        if (!((Binary) instr).left.isConst) {
                            setByLattice(((Binary) instr).left, findLattice(((Binary) instr).left));
                        }
                        if (!((Binary) instr).right.isConst) {
                            setByLattice(((Binary) instr).right, findLattice(((Binary) instr).right));
                        }
                    }
                } else if (instr instanceof Icmp) {
                    if (!findLattice(((Icmp) instr).output).removed()) {
                        newInstrList.add(instr);
                        if (!((Icmp) instr).left.isConst) {
                            setByLattice(((Icmp) instr).left, findLattice(((Icmp) instr).left));
                        }
                        if (!((Icmp) instr).right.isConst) {
                            setByLattice(((Icmp) instr).right, findLattice(((Icmp) instr).right));
                        }
                    }
                } else if (instr instanceof Load) {
                    if (!findLattice(((Load) instr).toVarName).removed()) {
                        newInstrList.add(instr);
                        ((Load) instr).fromPointer = findLattice(((Load) instr).fromPointer).getVar();
                    }
                } else if (instr instanceof Store) {
                    newInstrList.add(instr);
                    if (!((Store) instr).value.isConst) {
                        setByLattice(((Store) instr).value, findLattice(((Store) instr).value));
                        ((Store) instr).toPointer = findLattice(((Store) instr).toPointer).getVar();
                    }
                } else if (instr instanceof Call) {
                    newInstrList.add(instr);
                    for (var para : ((Call) instr).callList) {
                        if (!para.isConst) {
                            setByLattice(para, findLattice(para.varName));
                        }
                    }
                } else if (instr instanceof Getelementptr) {
                    if (!findLattice(((Getelementptr) instr).result).removed()) {
                        newInstrList.add(instr);
                        if (!((Getelementptr) instr).index.isConst) {
                            setByLattice(((Getelementptr) instr).index, findLattice(((Getelementptr) instr).index));
                        }
                        ((Getelementptr) instr).from = findLattice(((Getelementptr) instr).from).getVar();
                    }
                } else if (instr instanceof Ret) {
                    newInstrList.add(instr);
                    if (((Ret) instr).retVar != null && !((Ret) instr).retVar.isConst) {
                        setByLattice(((Ret) instr).retVar, findLattice(((Ret) instr).retVar));
                    }
                } else if (instr instanceof Br) {
                    newInstrList.add(instr);
                    var lattice = findLattice(((Br) instr).condition);
                    if (lattice != null && lattice.status == LatticeCell.CONST) {
                        if (lattice.constValue == 0) {
                            ((Br) instr).trueLabel = ((Br) instr).falseLabel;
                        }
                        ((Br) instr).condition = null;
                    }
                }
            }
            block.instrList = newInstrList;
        }
        funcDef.irList = newIrList;
    }

    private void setByLattice(Cell cell, LatticeCell latticeCell) {
        if (latticeCell.status == LatticeCell.CONST) {
            cell.set(latticeCell.constValue);
        } else {
            cell.set(latticeCell.getVar());
        }
    }
}