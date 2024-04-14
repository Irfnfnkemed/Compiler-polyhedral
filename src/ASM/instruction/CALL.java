package src.ASM.instruction;

import java.util.ArrayList;
import java.util.List;

public class CALL extends ASMInstr {
    public String func;
    public List<String> useList;//用到的虚拟寄存器(预染色为a0-a7)
    public List<ASMInstr> inlineCache;//内联插入
    public List<ASMInstr> paraList;//关于入参的指令列表
    public MV retMV;//关于返回值的移动指令

    public CALL(String func_) {
        func = func_;
        useList = new ArrayList<>();
        inlineCache = new ArrayList<>();
        paraList = new ArrayList<>();
    }
}
