package src.optimize.Mem2Reg;

import src.IR.IRProgram;
import src.IR.instruction.Br;
import src.IR.instruction.Instruction;
import src.IR.instruction.Phi;
import src.IR.statement.FuncDef;

import java.util.HashMap;
import java.util.Objects;

public class Mem2Reg {
    public HashMap<String, String> replaceLabel;//被合并的块名->目标的块名

    public Mem2Reg(IRProgram irProgram) {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                while (true) {
                    CFGDom cfg = new CFGDom((FuncDef) stmt);
                    Dom dom = new Dom(cfg);
                    PutPhi putPhi = new PutPhi(dom, (FuncDef) stmt);
                    if (dom.dfnList.size() > 4000) {
                        break;
                    }
                    if (putPhi.replace.size() == 0 && !cfg.change) {
                        //merge((FuncDef) stmt);//合并可以合并的块
                        break;
                    }
                }
            }
        }
    }

    private String find(String label) {
        String fatherLabel = replaceLabel.get(label);
        if (fatherLabel != null) {
            String findLabel = find(fatherLabel);
            replaceLabel.put(label, findLabel);
            return findLabel;
        }
        return label;
    }

//    private void merge(FuncDef stmt) {
//        CFGDom cfg = new CFGDom(stmt);
//        replaceLabel = new HashMap<>();
//        for (var block : cfg.funcBlocks.values()) {//合并能合并的块
//            if (block.pre == 1 && block.prev.get(0).suc == 1) {
//                replaceLabel.put(block.label, block.prev.get(0).label);
//            }
//        }
//        for (String label : replaceLabel.keySet()) {
//            find(label);
//        }
//        for (var entry : replaceLabel.entrySet()) {//合并能合并的块
//            if (Objects.equals(entry.getKey(), stmt.returnLabel)) {
//                stmt.returnLabel = cfg.funcBlocks.get(entry.getValue()).label;
//            }
//        }
//        stmt.irList.clear();
//        Label nowLabel;
//        String findLabel;
//        BlockDom nowBlockDom;
//        Instruction instr;
//        for (int i = 0; i < stmt.labelList.size(); ++i) {
//            nowLabel = stmt.labelList.get(i);
//            nowBlockDom = cfg.funcBlocks.get(nowLabel.labelName);
//            if (replaceLabel.containsKey(nowLabel.labelName)) {
//                stmt.labelList.remove(i--);
//                continue;
//            }
//            stmt.irList.add(nowLabel);
//            for (int j = 0; j < nowBlockDom.instructionList.size(); ++j) {
//                instr = nowBlockDom.instructionList.get(j);
//                if (instr instanceof Br) {
//                    String replace = replaceLabel.get(((Br) instr).nowLabel.substring(1));
//                    if (replace != null) {
//                        ((Br) instr).nowLabel = "%" + replace;
//                    }
//                    if (((Br) instr).condition == null) {
//                        findLabel = replaceLabel.get(((Br) instr).trueLabel.substring(1));
//                        if (findLabel != null) {
//                            nowBlockDom.instructionList.addAll(cfg.funcBlocks.get(((Br) instr).trueLabel.substring(1)).instructionList);
//                            continue;
//                        }
//                    }
//                }
//                stmt.irList.add(instr);
//                if (instr instanceof Phi) {
//                    for (var assign : ((Phi) instr).assignBlockList) {
//                        findLabel = replaceLabel.get(assign.label.substring(1));
//                        if (findLabel != null) {
//                            assign.label = "%" + findLabel;
//                        }
//                    }
//                }
//            }
//        }
//    }
}
