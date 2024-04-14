package src.ASM.instruction;

public class LA extends ASMInstr {
    public String to, fromLabel;

    public LA(String to_, String fromLabel_) {
        to = to_;
        fromLabel = fromLabel_;
    }
}
