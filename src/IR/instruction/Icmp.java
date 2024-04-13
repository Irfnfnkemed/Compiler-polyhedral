package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;
import src.Util.type.Type;

public class Icmp extends Instruction {
    public String cond;
    public IRType irType;
    public Cell left, right;
    public String output;
    private boolean onRight = true;

    public Icmp(String cond_, Type type_) {
        left = new Cell();
        right = new Cell();
        switch (cond_) {
            case "<" -> cond = "slt";
            case ">" -> cond = "sgt";
            case "<=" -> cond = "sle";
            case ">=" -> cond = "sge";
            case "==" -> cond = "eq";
            case "!=" -> cond = "ne";
        }
        irType = new IRType(type_);
    }

    public Icmp(String cond_, IRType irType_) {
        left = new Cell();
        right = new Cell();
        switch (cond_) {
            case "<" -> cond = "slt";
            case ">" -> cond = "sgt";
            case "<=" -> cond = "sle";
            case ">=" -> cond = "sge";
            case "==" -> cond = "eq";
            case "!=" -> cond = "ne";
        }
        irType = irType_;
    }

    public void set(Cell cell) {
        if (cell.isConst) {
            set(cell.varValue);
        } else {
            set(cell.varName);
        }
    }

    public void set(long value) {
        if (onRight) {
            right.set(value);
            onRight = false;
        } else {
            left.set(value);
        }
    }

    public void set(String var) {
        if (onRight) {
            right.set(var);
            onRight = false;
        } else {
            left.set(var);
        }
    }
}
