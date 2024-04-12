package src.IR;

import src.IR.instruction.Instruction;

import java.util.ArrayList;
import java.util.List;

abstract public class IRNode {
    public List<Instruction> cache;
    public boolean visited = false;

    public void pushCache(Instruction instr) {
        if (cache == null) {
            cache = new ArrayList<>();
        }
        cache.add(instr);
    }
}
