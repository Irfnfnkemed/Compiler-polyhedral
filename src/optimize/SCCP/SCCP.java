package src.optimize.SCCP;

import src.IR.IRProgram;
import src.IR.statement.FuncDef;

public class SCCP {

    public IRProgram irProgram;

    public SCCP(IRProgram irProgram_) {
        irProgram = irProgram_;
    }

    public void optimize() {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                FunctionSCCP functionSCCP = new FunctionSCCP((FuncDef) stmt);
                functionSCCP.optimize();
            }
        }
    }
}
