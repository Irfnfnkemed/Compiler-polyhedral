package src.optimize.Mem2Reg;

import src.IR.instruction.Instruction;
import src.IR.instruction.Phi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockDom {
    public List<Instruction> instructionList;
    public String label;
    public List<BlockDom> next;
    public List<BlockDom> prev;
    public int pre = 0, suc = 0;//前驱、后继个数
    public HashMap<String, Phi> insertPhi;//要插入的phi，局部变量名->phi指令
    public boolean renamed = false;//是否已经重命名过各操作
    public boolean visited = false;//用于建图时消除死块

    public BlockDom(String label_) {
        label = label_;
        instructionList = new ArrayList<>();
        next = new ArrayList<>();
        prev = new ArrayList<>();
        insertPhi = new HashMap<>();
    }

    public void pushIR(Instruction instruction) {
        instructionList.add(instruction);
    }

    public void setPre(BlockDom preBlockDom) {
        prev.add(preBlockDom);
        ++pre;
    }

    public void setSuc(BlockDom sucBlockDom) {
        next.add(sucBlockDom);
        ++suc;
    }

    public void setInverse() {
        int tmp = suc;
        suc = pre;
        pre = tmp;
        List<BlockDom> tmpList = new ArrayList<>(next);
        next.clear();
        next.addAll(prev);
        prev.clear();
        prev.addAll(tmpList);
    }
}
