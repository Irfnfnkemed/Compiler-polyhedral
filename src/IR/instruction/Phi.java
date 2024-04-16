package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;

import java.util.ArrayList;
import java.util.List;

public class Phi extends Instruction {
    public static class assignBlock {
        public Cell variable;
        public String label;

        public assignBlock(assignBlock assignBlock_) {
            variable = new Cell().set(assignBlock_.variable);
            label = assignBlock_.label;
        }

        public assignBlock(String var_, String label_) {
            variable = new Cell().set(var_);
            label = label_;
        }

        public assignBlock(long value_, String label_) {
            variable = new Cell().set(value_);
            label = label_;
        }

        public assignBlock(Cell cell_, String label_) {
            variable = new Cell().set(cell_);
            label = label_;
        }

    }

    public IRType irType;
    public String result;
    public List<assignBlock> assignBlockList;

    public Phi(Phi phi) {
        irType = new IRType(phi.irType);
        result = phi.result;
        assignBlockList = new ArrayList<>();
        phi.assignBlockList.forEach(assignBlock_ -> assignBlockList.add(new assignBlock(assignBlock_)));
    }

    public Phi(IRType irType_, String result_) {
        irType = irType_;
        result = result_;
        assignBlockList = new ArrayList<>();
    }

    public void push(Cell cell_, String label_) {
        assignBlockList.add(new assignBlock(cell_, label_));
    }

    public void push(String var_, String label_) {
        assignBlockList.add(new assignBlock(var_, label_));
    }

    public void push(long value_, String label_) {
        assignBlockList.add(new assignBlock(value_, label_));
    }

}
