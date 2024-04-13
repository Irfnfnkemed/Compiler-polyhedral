package src.IR;

import src.IR.instruction.Block;
import src.IR.instruction.Instruction;
import src.IR.statement.FuncDef;
import src.Util.cell.Cell;

import java.util.Stack;

public class Exp extends IRNode {

    public Stack<Cell> varStack;
    public FuncDef funcDef;
    public String lhsVar;//指向存放左值的空间的指针名

    public Exp(FuncDef funcDef_) {
        funcDef = funcDef_;
        varStack = new Stack<>();
    }

    public void set(long value) {
        varStack.push(new Cell().set(value));
    }

    public void set(boolean value) {
        varStack.push(new Cell().set(value ? 1L : 0L));
    }

    public void set(String anonymousVar) {
        varStack.push(new Cell().set(anonymousVar));
    }

    public void set(Cell cell) {
        varStack.push(new Cell().set(cell));
    }

    public boolean isOperandConst() {
        return varStack.peek().isConst;
    }

    public boolean isOperandTwoConst() {
        boolean flag = false;
        var tmp = varStack.pop();
        if (tmp.isConst && varStack.peek().isConst) {
            flag = true;
        }
        varStack.push(tmp);
        return flag;
    }

    public Cell pop() {
        return varStack.pop();
    }

    public String getVar() {
        return varStack.peek().varName;
    }

    public void push(Instruction instruction) {
        funcDef.push(instruction);
    }

    public void pushBlock(Block block) {
        funcDef.pushBlock(block);
    }
}
