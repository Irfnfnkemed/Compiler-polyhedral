package src.ASM.instruction;

public class SW extends ASMInstr {
    public String from, to;
    public int offset;//offset为-2，表示改为MV

    public SW(String from_, String to_, int offset_) {
        from = from_;
        to = to_;
        offset = offset_;
    }
}
