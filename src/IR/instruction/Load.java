package src.IR.instruction;

import src.Util.type.IRType;
import src.Util.type.Type;

public class Load extends Instruction {
    public IRType irType;
    public String toVarName;//存到匿名变量里
    public String fromPointer;

    public Load(Type type_, String toVarName_, String fromPointer_) {
        irType = new IRType(type_);
        toVarName = toVarName_;
        fromPointer = fromPointer_;
    }

    public Load(IRType irType_, String toVarName_, String fromPointer_) {
        irType = irType_;
        toVarName = toVarName_;
        fromPointer = fromPointer_;
    }
}

