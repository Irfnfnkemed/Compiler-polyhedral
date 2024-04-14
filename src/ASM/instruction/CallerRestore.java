package src.ASM.instruction;

import java.util.ArrayList;
import java.util.List;

public class CallerRestore extends ASMInstr {
    public CallerSave callerSave;
    public String funcName;//调用的函数名
    public List<ASMInstr> callerList;

    public CallerRestore(CallerSave callerSave_, String funcName_) {
        callerSave = callerSave_;
        funcName = funcName_;
        callerList = new ArrayList<>();
    }
}
