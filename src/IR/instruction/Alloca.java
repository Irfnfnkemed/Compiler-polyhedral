package src.IR.instruction;

import src.Util.type.IRType;
import src.Util.type.Type;

public class Alloca extends Instruction {
    public IRType irType;
    public String varName;

    public Alloca(Type type_, String varName_) {
        irType = new IRType(type_);
        varName = varName_;
    }

    public Alloca(IRType irType_, String varName_) {
        irType = irType_;
        varName = varName_;
    }
}
