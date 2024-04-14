package src.ASM.instruction;

import java.util.ArrayList;
import java.util.List;

public class Init extends ASMInstr {
    public List<ASMInstr> initList;
    public List<ASMInstr> paraList;
    public ASMInstr retInstr;

    public Init() {
        initList = new ArrayList<>();
        paraList = new ArrayList<>();
    }
}
