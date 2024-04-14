package src.ASM.instruction;

import src.ASM.instruction.binaryImme.ADDI;

import java.util.ArrayList;
import java.util.List;

public class Restore extends ASMInstr {
    public List<ASMInstr> restoreList;
    public Restore(){
        restoreList = new ArrayList<>();
    }


    public void set(Init init) {
        for (int i = init.initList.size() - 1; i >= 0; --i) {
            var instr = init.initList.get(i);
            if (instr instanceof SW) {
                restoreList.add(new LW("sp", ((SW) instr).from, ((SW) instr).offset));
            } else if (instr instanceof ADDI) {
                restoreList.add(new ADDI("sp", "sp", -((ADDI) instr).imme));
            }
        }
    }
}
