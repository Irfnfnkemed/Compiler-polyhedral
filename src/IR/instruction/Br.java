package src.IR.instruction;

import src.IR.statement.FuncDef;

public class Br extends Instruction {
    public String condition = null;
    public String trueLabel;
    public String falseLabel;
    public String nowLabel;

    public Br(Br br) {
        condition = br.condition;
        trueLabel = br.trueLabel;
        falseLabel = br.falseLabel;
        nowLabel = br.nowLabel;
    }

    public Br(String toLabel_, FuncDef funcDef_) {
        trueLabel = toLabel_;
        nowLabel = funcDef_.nowLabel();
    }

    public Br(String condition_, String trueLabel_, String falseLabel_, FuncDef funcDef_) {
        condition = condition_;
        trueLabel = trueLabel_;
        falseLabel = falseLabel_;
        nowLabel = funcDef_.nowLabel();
    }

    public Br(String fromLabel, String toLabel_) {
        trueLabel = "%" + toLabel_;
        nowLabel = "%" + fromLabel;
        condition = null;
    }
}
