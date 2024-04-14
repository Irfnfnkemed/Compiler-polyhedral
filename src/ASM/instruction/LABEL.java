package src.ASM.instruction;

public class LABEL extends ASMInstr {
    public String label;
    public boolean isFuncBeg = false;

    public LABEL(String label_) {
        label = label_;
    }

    public LABEL(String label_, boolean isFuncBeg_) {
        label = label_;
        isFuncBeg = isFuncBeg_;
    }
}
