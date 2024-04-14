package src.optimize.RegAllocation;

import src.ASM.instruction.ASMInstr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockReg {
    public static class BlockLive {
        public Set<String> blockUse;
        public Set<String> blockDef;
        public Set<String> liveIn;
        public Set<String> liveOut;
        public Set<String> liveOutBackup;

        public BlockLive() {
            blockUse = new HashSet<>();
            blockDef = new HashSet<>();
            liveIn = new HashSet<>();
            liveOut = new HashSet<>();
            liveOutBackup = new HashSet<>();
        }

        public void addDef(String varName) {
            blockDef.add(varName);
        }

        public void addUse(String varName) {
            if (!blockDef.contains(varName)) {
                blockUse.add(varName);
            }
        }
    }


    public List<ASMInstr> instructionList;
    public String label;
    public BlockReg[] next;
    public List<BlockReg> prev;
    public BlockLive blockLive;
    public int pre = 0, suc = 0;//前驱、后继个数


    public BlockReg(String label_) {
        label = label_;
        instructionList = new ArrayList<>();
        next = new BlockReg[2];
        prev = new ArrayList<>();
        blockLive = new BlockLive();
    }

    public void pushASM(ASMInstr asmInstr) {
        instructionList.add(asmInstr);
    }

    public void setPre(BlockReg preBlockDom) {
        prev.add(preBlockDom);
        ++pre;
    }

    public void setSuc(BlockReg sucBlockDom) {
        next[suc++] = sucBlockDom;
    }

}


