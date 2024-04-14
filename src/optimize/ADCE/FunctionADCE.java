package src.optimize.ADCE;

import src.IR.instruction.*;
import src.IR.statement.FuncDef;
import src.Util.cell.Cell;
import src.optimize.Mem2Reg.Dom;

import java.util.*;

public class FunctionADCE {
    public HashMap<String, Instruction> defSet;//变量名->定义
    public HashMap<String, List<Br>> domBr;
    public HashSet<String> activeBlock;
    public HashMap<String, Br> blockBr;//块名->结尾Br
    public Queue<Instruction> activeInstr;
    public Dom dom;
    public FuncDef funcDef;

    public FunctionADCE(Dom dom_) {
        activeInstr = new ArrayDeque<>();
        defSet = new HashMap<>();
        domBr = new HashMap<>();
        activeBlock = new HashSet<>();
        activeBlock.add("entry");
        blockBr = new HashMap<>();
        dom = dom_;
        funcDef = dom_.cfgDom.funcDef;
        setDefAndActive();
        setBrDom();
        markActive();
        rebuildBr();
        recollect();
    }

    private void setDefAndActive() {
        for (var block : funcDef.irList) {
            for (var instr : block.instrList) {
                instr.extraBase = new ExtraADCE();
                if (instr instanceof Br) {
                    blockBr.put(((Br) instr).nowLabel.substring(1), ((Br) instr));
                } else if (instr instanceof Binary) {
                    defSet.put(((Binary) instr).output, instr);
                } else if (instr instanceof Icmp) {
                    defSet.put(((Icmp) instr).output, instr);
                } else if (instr instanceof Load) {
                    defSet.put(((Load) instr).toVarName, instr);
                } else if (instr instanceof Store) {
                    activeInstr.add(instr);
                    ((ExtraADCE) instr.extraBase).isActive = true;
                    activeBlock.add(block.label);
                } else if (instr instanceof Call) {
                    if (((Call) instr).resultVar != null) {
                        defSet.put(((Call) instr).resultVar, instr);
                    }
                    activeInstr.add(instr);
                    ((ExtraADCE) instr.extraBase).isActive = true;
                    activeBlock.add(block.label);
                } else if (instr instanceof Phi) {
                    defSet.put(((Phi) instr).result, instr);
                } else if (instr instanceof Getelementptr) {
                    defSet.put(((Getelementptr) instr).result, instr);
                } else if (instr instanceof Ret) {
                    activeInstr.add(instr);
                    ((ExtraADCE) instr.extraBase).isActive = true;
                    activeBlock.add(block.label);
                }
                ((ExtraADCE) instr.extraBase).block = block;
            }
        }
    }

    private void setBrDom() {
        for (var block : dom.domMap.values()) {
            List<Br> tmp = new ArrayList<>();
            for (String domFrontier : block.domFrontier) {
                tmp.add(blockBr.get(domFrontier));
            }
            domBr.put(block.blockName, tmp);
        }
    }

    private void markActive() {
        while (!activeInstr.isEmpty()) {
            Instruction instr = activeInstr.poll();
            if (!((ExtraADCE) instr.extraBase).isVisited) {
                ((ExtraADCE) instr.extraBase).isVisited = true;
                setDomInstr(((ExtraADCE) instr.extraBase).block.label);
                if (instr instanceof Br) {
                    setActiveInstr(((Br) instr).condition);
                    activeBlock.add(((Br) instr).trueLabel.substring(1));
                    if (((Br) instr).falseLabel != null) {
                        activeBlock.add(((Br) instr).falseLabel.substring(1));
                    }
                } else if (instr instanceof Binary) {
                    setActiveInstr(((Binary) instr).left);
                    setActiveInstr(((Binary) instr).right);
                } else if (instr instanceof Icmp) {
                    setActiveInstr(((Icmp) instr).left);
                    setActiveInstr(((Icmp) instr).right);
                } else if (instr instanceof Load) {
                    setActiveInstr(((Load) instr).fromPointer);
                } else if (instr instanceof Store) {
                    setActiveInstr(((Store) instr).value);
                    setActiveInstr(((Store) instr).toPointer);
                } else if (instr instanceof Call) {
                    for (var para : ((Call) instr).callList) {
                        setActiveInstr(para.varName);
                    }
                } else if (instr instanceof Phi) {
                    for (var assign : ((Phi) instr).assignBlockList) {
                        setActiveInstr(assign.variable);
                        Br pre_br = blockBr.get(assign.label.substring(1));
                        activeInstr.add(pre_br);
                        ((ExtraADCE) pre_br.extraBase).isActive = true;
                    }
                } else if (instr instanceof Getelementptr) {
                    setActiveInstr(((Getelementptr) instr).from);
                    setActiveInstr(((Getelementptr) instr).index);
                } else if (instr instanceof Ret) {
                    setActiveInstr(((Ret) instr).retVar);
                }
            }
        }
    }

    private void addDomBr(String label, Br br) {
        var tmp = domBr.computeIfAbsent(label, k -> new ArrayList<>());
        tmp.add(br);
    }

    private void setActiveInstr(String varName) {
        if (varName == null || varName.charAt(0) == '@') {
            return;
        }
        Instruction def = defSet.get(varName);
        if (def != null && !((ExtraADCE) def.extraBase).isVisited) {
            activeInstr.add(defSet.get(varName));//加入定义
            ((ExtraADCE) def.extraBase).isActive = true;
        }
    }

    private void setActiveInstr(Cell cell) {
        if (cell == null || cell.isConst) {
            return;
        }
        setActiveInstr(cell.varName);
    }

    private void setDomInstr(String nowLabel) {
        activeBlock.add(nowLabel);
        var brList = domBr.get(nowLabel);
        if (brList != null) {
            for (Br br : brList) {//添加控制依赖
                if (!((ExtraADCE) br.extraBase).isVisited) {
                    activeInstr.add(br);
                    ((ExtraADCE) br.extraBase).isActive = true;
                }
            }
        }
    }

    private void rebuildBr() {
        for (Br br : blockBr.values()) {
            if (activeBlock.contains(((ExtraADCE) br.extraBase).block.label) && !((ExtraADCE) br.extraBase).isActive) {
                String label = dom.domMap.get(((ExtraADCE) br.extraBase).block.label).immeDom.blockName;
                while (!activeBlock.contains(label)) {
                    label = dom.domMap.get(label).immeDom.blockName;
                }
                br.condition = null;
                br.trueLabel = "%" + label;
                ((ExtraADCE) br.extraBase).isActive = true;
            }
        }
    }

    private void recollect() {
        List<Block> newIrList = new ArrayList<>();
        for (Block block : funcDef.irList) {
            List<Instruction> newInstrList = new ArrayList<>();
            for (Instruction instr : block.instrList) {
                if (((ExtraADCE) instr.extraBase).isActive) {
                    newInstrList.add(instr);
                }
            }
            if (!newInstrList.isEmpty()) {
                block.instrList = newInstrList;
                newIrList.add(block);
            }
        }
        funcDef.irList = newIrList;
    }
}
