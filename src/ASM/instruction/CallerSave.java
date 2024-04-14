package src.ASM.instruction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CallerSave extends ASMInstr {
    public HashSet<String> varName;//调用前活跃的虚拟寄存器
    public HashSet<String> callerReg;//需要保存的寄存器
    public List<SW> callerList;
    public int paraSize = 0;

    public CallerSave(int paraSize_) {
        varName = new HashSet<>();
        callerReg = new HashSet<>();
        paraSize = paraSize_;
        callerList = new ArrayList<>();
    }

    public void setCallerReg(String reg) {
        if (reg == null) {
            return;
        }
        if (reg.charAt(0) == 's') {
            return;
        }
        if (reg.charAt(0) == 'a' && reg.charAt(1) < '0' + paraSize) {
            return;
        }
        callerReg.add(reg);
    }
}
