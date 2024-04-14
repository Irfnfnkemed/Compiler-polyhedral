package src.optimize.ADCE;


import src.IR.IRProgram;
import src.IR.statement.FuncDef;
import src.optimize.Mem2Reg.CFGDom;
import src.optimize.Mem2Reg.Dom;

public class ADCE {
    public IRProgram irProgram;

    public ADCE(IRProgram irProgram_) {
        irProgram = irProgram_;
    }


    public void optimize() {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                CFGDom cfg = new CFGDom((FuncDef) stmt, false);
                if (cfg.noReturn) {
                    return;//死循环
                }
                if (cfg.funcBlockDoms.size() == 1) {
                    continue;//只有一个块，不需要ADCE
                }
                cfg.inverse();
                Dom dom = new Dom(cfg, cfg.retLabel);
                FunctionADCE functionADCE = new FunctionADCE(dom);
                functionADCE.optimize();
            }
        }
    }
}
