package src.optimize.Mem2Reg;

import src.IR.instruction.*;
import src.IR.statement.FuncDef;
import src.Util.type.IRType;

import java.util.*;

public class CFGDom {

    public HashMap<String, BlockDom> funcBlockDoms;//块名->Block节点
    public HashMap<String, List<String>> allocaVar;//alloca的变量，变量名->def的块名列表
    public HashMap<String, IRType> allocaVarType;//alloca的变量名->类型
    public boolean change = false;//控制流发生改变
    public boolean noReturn = false;//是否一定死循环
    public FuncDef funcDef;
    public String retLabel;

    public CFGDom(FuncDef funcDef_, boolean isAlloca) {
        funcDef = funcDef_;
        funcBlockDoms = new HashMap<>();
        allocaVar = new HashMap<>();
        allocaVarType = new HashMap<>();
        buildCFG(isAlloca);
        eliminateBlock();
    }

    public void buildCFG(boolean isAlloca) {
        for (var block : funcDef.irList) {
            funcBlockDoms.put(block.label, new BlockDom(block));
        }
        for (var block : funcDef.irList) {//建图
            var nowBlockDomDom = funcBlockDoms.get(block.label);
            if (isAlloca) {
                for (Instruction instr : block.instrList) {
                    if (instr instanceof Alloca) {//收集alloca信息
                        allocaVar.put(((Alloca) instr).varName, new ArrayList<>());
                        allocaVarType.put(((Alloca) instr).varName, ((Alloca) instr).irType);
                    } else if (instr instanceof Store) {
                        if (allocaVar.containsKey(((Store) instr).toPointer)) {
                            var defList = allocaVar.get(((Store) instr).toPointer);
                            if (defList.isEmpty() || !Objects.equals(defList.get(defList.size() - 1), nowBlockDomDom.block.label)) {
                                allocaVar.get(((Store) instr).toPointer).add(nowBlockDomDom.block.label);
                            }
                        }
                    }
                }
            }
            Instruction endInstr = block.instrList.get(block.instrList.size() - 1);
            if (endInstr instanceof Br) {
                var nextBlock = funcBlockDoms.get(((Br) endInstr).trueLabel.substring(1));
                nextBlock.setPre(nowBlockDomDom);
                nowBlockDomDom.setSuc(nextBlock);
                if (((Br) endInstr).condition != null) {
                    nextBlock = funcBlockDoms.get(((Br) endInstr).falseLabel.substring(1));
                    nowBlockDomDom.setSuc(nextBlock);
                    nextBlock.setPre(nowBlockDomDom);
                }
            } else {
                retLabel = block.label;
            }
        }
    }

    public void eliminateBlock() {
        Queue<BlockDom> queue = new ArrayDeque<>();
        queue.add(funcBlockDoms.get("entry"));
        BlockDom blockDom;
        while (!queue.isEmpty()) {//BFS
            blockDom = queue.poll();
            blockDom.visited = true;
            for (int i = 0; i < blockDom.suc; ++i) {
                if (!(blockDom.next.get(i)).visited) {
                    queue.add(blockDom.next.get(i));
                }
            }
        }
        var iterator = funcBlockDoms.values().iterator();
        while (iterator.hasNext()) {//初步消除死块
            var entry = iterator.next();
            if (!(entry).visited) {
                for (int i = 0; i < entry.suc; ++i) {
                    blockDom = entry.next.get(i);
                    for (int j = 0; j < blockDom.pre; ++j) {
                        if (blockDom.prev.get(j) == entry) {
                            blockDom.prev.remove(j);
                            --blockDom.pre;
                            break;
                        }
                    }
                }
                if (Objects.equals(entry.block.label, retLabel)) {
                    noReturn = true;
                }
                iterator.remove();
                change = true;
            }
        }
        if (retLabel == null) {
            noReturn = true;
        }
    }

    public void inverse() {//建立反图
        for (BlockDom blockDom : funcBlockDoms.values()) {
            blockDom.setInverse();
        }
    }

}
