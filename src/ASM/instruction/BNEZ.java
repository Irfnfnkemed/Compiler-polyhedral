package src.ASM.instruction;

public class BNEZ extends ASMInstr {
    public String condition, toLabel;

    public BNEZ(String condition_, String toLabel_) {
        condition = condition_;
        toLabel = toLabel_;
    }
}
