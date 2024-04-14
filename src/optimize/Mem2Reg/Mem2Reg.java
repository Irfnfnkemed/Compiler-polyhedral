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
}
