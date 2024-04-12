package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;
import src.Util.type.Type;

import java.util.ArrayList;
import java.util.List;

public class Call extends Instruction {

    public IRType irType;
    public String functionName;
    public List<IRType> callTypeList;
    public List<Cell> callList;
    public String resultVar;

    public Call(String functionName_) {
        functionName = functionName_;
        callTypeList = new ArrayList<>();
        callList = new ArrayList<>();
    }

    public void set(IRType irType, String anonymousVar) {
        callTypeList.add(irType);
        callList.add(new Cell().set(anonymousVar));
    }

    public void set(IRType irType, long value) {
        callTypeList.add(irType);
        callList.add(new Cell().set(value));
    }

    public void set(Type type, String anonymousVar) {
        set(new IRType(type), anonymousVar);
    }

    public void set(Type type, long value) {
        set(new IRType(type), value);
    }

    public void set(IRType irType, Cell var) {
        if (var.isConst) {
            set(irType, var.varValue);
        } else {
            set(irType, var.varName);
        }
    }

    public void set(Type type, Cell var) {
        set(new IRType(type), var);
    }

}
