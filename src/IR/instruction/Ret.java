package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;
import src.Util.type.Type;

public class Ret extends Instruction {
    public IRType irType;
    public Cell retVar;

    public Ret() {
    }

    public Ret(Type type_, String var_) {
        retVar = new Cell().set(var_);
        irType = new IRType(type_);
    }

    public Ret(IRType irType_, String var_) {
        retVar = new Cell().set(var_);
        irType = irType_;
    }
}
