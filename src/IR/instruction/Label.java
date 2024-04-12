package src.IR.instruction;

public class Label extends Instruction {
    public String labelName;

    public Label(String labelName_) {
        labelName = labelName_;
    }
}
