package src.IR.instruction;

import src.IR.IRNode;

import java.util.ArrayList;
import java.util.List;

public class Block extends IRNode {
    public String label;
    public List<Instruction> instrList;

    public Block(String label_) {
        label = label_;
        instrList = new ArrayList<>();
    }
}
