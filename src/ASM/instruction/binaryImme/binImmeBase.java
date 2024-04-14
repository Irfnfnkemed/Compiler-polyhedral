package src.ASM.instruction.binaryImme;

import src.ASM.instruction.ASMInstr;

public class binImmeBase extends ASMInstr {
    public String to, from;
    public int imme;

    public binImmeBase(String to_, String from_, int imme_) {
        to = to_;
        from = from_;
        imme = imme_;
    }
}
