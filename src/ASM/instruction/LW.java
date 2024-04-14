package src.ASM.instruction;

public class LW extends ASMInstr {
    public String from, to;
    public int offset;//为-1，表示直接从目标标签lw；为-2，表示改成MV

    public LW(String from_, String to_, int offset_) {
        from = from_;
        to = to_;
        offset = offset_;
    }

    public LW(String from_, String to_) {
        from = from_;
        to = to_;
        offset = -1;//表示直接从目标标签lw
    }
}
