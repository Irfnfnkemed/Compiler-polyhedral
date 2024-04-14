package src.ASM.instruction;

public class MV extends ASMInstr {
    public String from, to;

    public MV(String from_, String to_) {
        from = from_;
        to = to_;
    }
}
