package src.IR.instruction;

import src.Util.cell.Cell;

public class Binary extends Instruction {
    public Cell left, right;
    public String output;
    public String op;

    private boolean onRight = true;

    public Binary(String op_) {
        left = new Cell();
        right = new Cell();
        switch (op_) {
            case "+" -> op = "add";
            case "-" -> op = "sub";
            case "*" -> op = "mul";
            case "/" -> op = "sdiv";
            case "%" -> op = "srem";
            case "<<" -> op = "shl";
            case ">>" -> op = "ashr";
            case "&" -> op = "and";
            case "|" -> op = "or";
            case "^" -> op = "xor";
        }
    }

    public Binary(Binary binary) {
        left = new Cell().set(binary.left);
        right = new Cell().set(binary.right);
        op = binary.op;
        output = binary.output;
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
