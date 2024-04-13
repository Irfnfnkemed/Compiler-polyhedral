package src.optimize.Mem2Reg;

import src.IR.IRProgram;
import src.IR.instruction.Block;
import src.IR.instruction.Br;
import src.IR.instruction.Phi;
import src.IR.statement.FuncDef;

import java.util.ArrayList;
import java.util.List;

public class Mem2Reg {

    public Mem2Reg(IRProgram irProgram) {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                boolean isAlloca = true;
                while (true) {
                    CFGDom cfg = new CFGDom((FuncDef) stmt, isAlloca);
                    isAlloca = false;
                    Dom dom = new Dom(cfg);
                    PutPhi putPhi = new PutPhi(dom, (FuncDef) stmt);
                    if (dom.dfnList.size() > 4000 || cfg.noReturn) {
                        break;
                    }
                    if (putPhi.replace.size() == 0 && !cfg.change) {
                        merge((FuncDef) stmt, cfg);
                        break;
                    }
                }
            }
        }
    }

    public void merge(FuncDef funcDef, CFGDom cfg) {
        List<Block> newIrList = new ArrayList<>();
        for (var block : funcDef.irList) {
            var domBlock = cfg.funcBlockDoms.get(block.label);
            if (domBlock == null) {
                continue;
            }
            if (domBlock.pre == 1 && domBlock.prev.get(0).suc == 1) {
                var preDomBlock = domBlock.prev.get(0);
                preDomBlock.block.instrList.remove(preDomBlock.block.instrList.size() - 1);//移除br
                preDomBlock.block.instrList.addAll(domBlock.block.instrList);
                preDomBlock.next = domBlock.next;
                preDomBlock.suc = domBlock.suc;
                var endInstr = preDomBlock.block.instrList.get(preDomBlock.block.instrList.size() - 1);
                if (endInstr instanceof Br) {
                    ((Br) endInstr).nowLabel = "%" + preDomBlock.block.label;
                }
                for (var sucBlock : domBlock.next) {
                    for (int i = 0; i < sucBlock.pre; ++i) {
                        if (sucBlock.prev.get(i) == domBlock) {
                            sucBlock.prev.set(i, preDomBlock);
                            break;
                        }
                    }
                    for (var instr : sucBlock.block.instrList) {
                        if (instr instanceof Phi) {
                            for (var assign : ((Phi) instr).assignBlockList) {
                                if (assign.label.substring(1).equals(block.label)) {
                                    assign.label = "%" + preDomBlock.block.label;
                                    break;
                                }
                            }
                        }
                    }
                }
                cfg.funcBlockDoms.remove(block.label);
            } else {
                newIrList.add(block);
            }
        }
        funcDef.irList = newIrList;
    }


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
    //}
}
