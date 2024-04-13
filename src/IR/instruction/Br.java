package src.IR.instruction;

import src.IR.statement.FuncDef;

public class Br extends Instruction {
    public String condition;
    public String trueLabel;
    public String falseLabel;
    public FuncDef funcDef;
    public String nowLabel;

    public Br(String toLabel_, FuncDef funcDef_) {
        trueLabel = toLabel_;
        funcDef = funcDef_;
        nowLabel = funcDef_.nowLabel();
    }

    public Br(String condition_, String trueLabel_, String falseLabel_,FuncDef funcDef_) {
        condition = condition_;
        trueLabel = trueLabel_;
        falseLabel = falseLabel_;
        funcDef= funcDef_;
        nowLabel = funcDef_.nowLabel();
    }
}
