package src.ASM.instruction.binary;

import src.ASM.instruction.ASMInstr;

public class binBase extends ASMInstr {
    public String lhs, rhs, to;

    public binBase(String lhs_, String rhs_, String to_) {
        lhs = lhs_;
        rhs = rhs_;
        to = to_;
    }
}
