package src.optimize.RegAllocation;

import src.ASM.instruction.*;
import src.ASM.instruction.binary.binBase;
import src.ASM.instruction.binaryImme.binImmeBase;

import java.util.*;

public class CFGReg {
    public HashMap<String, BlockReg> blocks;//块名->Block节点
    public BlockReg outBlock;//出口(出度为0)
    public List<ASMInstr> asmInstrList;
    public HashSet<String> globalVar;

    public CFGReg(List<ASMInstr> asmInstrList_, HashSet<String> globalVar_) {
        asmInstrList = asmInstrList_;
        globalVar = globalVar_;
        blocks = new HashMap<>();
        buildCFG();
        getBlockUseDef();
        getBlockInOut();
    }

    public void buildCFG() {
        BlockReg nowBlockReg = null;
        for (int i = 0; i < asmInstrList.size(); ++i) {//建图
            var instr = asmInstrList.get(i);
            if (instr.ignore) {
                asmInstrList.remove(i--);
                continue;
            }
            if (instr instanceof LABEL) {
                nowBlockReg = blocks.get(((LABEL) instr).label);
                if (nowBlockReg == null) {
                    nowBlockReg = new BlockReg(((LABEL) instr).label);
                    blocks.put(nowBlockReg.label, nowBlockReg);
                }
            } else {
                assert nowBlockReg != null;
                nowBlockReg.pushASM(instr);
                if (instr instanceof BNEZ) {
                    var nextBlock = blocks.get(((BNEZ) instr).toLabel);
                    if (nextBlock == null) {
                        nextBlock = new BlockReg(((BNEZ) instr).toLabel);
                        blocks.put(nextBlock.label, nextBlock);
                    }
                    nextBlock.setPre(nowBlockReg);
                    nowBlockReg.setSuc(nextBlock);
                } else if (instr instanceof J) {
                    var nextBlock = blocks.get(((J) instr).toLabel);
                    if (nextBlock == null) {
                        nextBlock = new BlockReg(((J) instr).toLabel);
                        blocks.put(nextBlock.label, nextBlock);
                    }
                    nextBlock.setPre(nowBlockReg);
                    nowBlockReg.setSuc(nextBlock);
                } else if (instr instanceof CALL) {
                    if (((CALL) instr).inlineCache.size() > 0) {
                        nowBlockReg.instructionList.remove(nowBlockReg.instructionList.size() - 1);//移除call
                        nowBlockReg.instructionList.remove(nowBlockReg.instructionList.size() - 1);//移除callerSave
                        asmInstrList.remove(i--);
                        asmInstrList.remove(i);
                        for (var inlineInstr : ((CALL) instr).inlineCache) {
                            asmInstrList.add(i++, inlineInstr);
                            if (inlineInstr instanceof LABEL) {
                                nowBlockReg = blocks.get(((LABEL) inlineInstr).label);
                                if (nowBlockReg == null) {
                                    nowBlockReg = new BlockReg(((LABEL) inlineInstr).label);
                                    blocks.put(nowBlockReg.label, nowBlockReg);
                                }
                            } else {
                                if (inlineInstr instanceof BNEZ) {
                                    var nextBlock = blocks.get(((BNEZ) inlineInstr).toLabel);
                                    if (nextBlock == null) {
                                        nextBlock = new BlockReg(((BNEZ) inlineInstr).toLabel);
                                        blocks.put(nextBlock.label, nextBlock);
                                    }
                                    nextBlock.setPre(nowBlockReg);
                                    nowBlockReg.setSuc(nextBlock);
                                } else if (inlineInstr instanceof J) {
                                    var nextBlock = blocks.get(((J) inlineInstr).toLabel);
                                    if (nextBlock == null) {
                                        nextBlock = new BlockReg(((J) inlineInstr).toLabel);
                                        blocks.put(nextBlock.label, nextBlock);
                                    }
                                    nextBlock.setPre(nowBlockReg);
                                    nowBlockReg.setSuc(nextBlock);
                                }
                            }
                            nowBlockReg.pushASM(inlineInstr);
                        }
                        if (((CALL) instr).retMV != null) {
                            if (asmInstrList.get(i).ignore) {
                                asmInstrList.remove(i);
                            } else {
                                nowBlockReg.pushASM(asmInstrList.get(i++));
                            }
                        }
                        asmInstrList.remove(i--);
                    }
                }
            }
        }
    }


    public void getBlockUseDef() {
        BlockReg nowBlock = null;//得到每个块的等效use和def
        for (var entry : blocks.entrySet()) {
            nowBlock = entry.getValue();
            for (var instr : nowBlock.instructionList) {
                setInstrUseDef(instr, nowBlock);
            }
            nowBlock.blockLive.liveIn.addAll(nowBlock.blockLive.blockUse);
            if (nowBlock.suc == 0) {
                outBlock = nowBlock;
            }
        }
        if (outBlock == null) {//无出度为0的节点，实际上会死循环
            outBlock = nowBlock;
        }
    }

    public void getBlockInOut() {
        BlockReg blockReg;
        Queue<BlockReg> queue = new ArrayDeque<>();
        HashSet<BlockReg> visited = new HashSet<>();
        boolean flag = true;
        while (flag) {
            flag = false;
            queue.add(outBlock);
            visited.clear();
            while (!queue.isEmpty()) {
                blockReg = queue.poll();
                for (int i = 0; i < blockReg.suc; ++i) {
                    if (blockReg.blockLive.liveOut.addAll(blocks.get(blockReg.next[i].label).blockLive.liveIn)) {
                        flag = true;
                    }
                }
                for (String var : blockReg.blockLive.liveOut) {
                    if (!blockReg.blockLive.blockDef.contains(var)) {
                        if (blockReg.blockLive.liveIn.add(var)) {
                            flag = true;
                        }
                    }
                }
                for (int i = 0; i < blockReg.pre; ++i) {
                    if (!visited.contains(blockReg.prev.get(i))) {
                        queue.add(blockReg.prev.get(i));
                        visited.add(blockReg.prev.get(i));
                    }
                }
            }
        }
        for (var block : blocks.values()) {
            block.blockLive.liveOutBackup.clear();
            block.blockLive.liveOutBackup.addAll(block.blockLive.liveOut);//备份
        }
    }

    public void reset(HashSet<String> spiltSet) {
        for (var block : blocks.values()) {
            block.instructionList.clear();
            block.blockLive.liveOut.clear();
            block.blockLive.liveOutBackup.removeAll(spiltSet);
            block.blockLive.liveOut.addAll(block.blockLive.liveOutBackup);//备份
        }
        BlockReg nowBlockReg = null;
        for (var instr : asmInstrList) {//更新图
            if (instr instanceof LABEL) {
                nowBlockReg = blocks.get(((LABEL) instr).label);
            } else {
                assert nowBlockReg != null;
                nowBlockReg.pushASM(instr);
            }
            if (!instr.visited) {
                setInstrUseDef(instr, nowBlockReg);
            }
        }
    }

    void setInstrUseDef(ASMInstr instr, BlockReg nowBlock) {
        instr.visited = true;
        if (instr instanceof LI) {
            if (instr.preColoredTo == null) {
                nowBlock.blockLive.addDef(((LI) instr).to);
            }
            instr.def = ((LI) instr).to;
        } else if (instr instanceof LW) {
            instr.useNum = 0;
            if (((LW) instr).offset != -1 && !((LW) instr).from.contains("#")) {
                nowBlock.blockLive.addUse(((LW) instr).from);
                instr.use[0] = ((LW) instr).from;
                instr.useNum = 1;
            }
            nowBlock.blockLive.addDef(((LW) instr).to);
            instr.def = ((LW) instr).to;
        } else if (instr instanceof LA) {
            nowBlock.blockLive.addDef(((LA) instr).to);
            instr.def = ((LA) instr).to;
        } else if (instr instanceof SW) {
            instr.useNum = 0;
            if (!Objects.equals(((SW) instr).from, "zero")) {
                nowBlock.blockLive.addUse(((SW) instr).from);
                instr.use[instr.useNum++] = ((SW) instr).from;
            }
            if (!((SW) instr).to.contains("#")) {
                nowBlock.blockLive.addUse(((SW) instr).to);
                instr.use[instr.useNum++] = ((SW) instr).to;
            }
        } else if (instr instanceof MV) {
            instr.useNum = 0;
            if (!Objects.equals(((MV) instr).from, "zero")) {
                if (instr.preColoredFrom == null) {
                    nowBlock.blockLive.addUse(((MV) instr).from);
                }
                instr.use[instr.useNum++] = ((MV) instr).from;
            }
            if (instr.preColoredTo == null) {
                nowBlock.blockLive.addDef(((MV) instr).to);
            }
            instr.def = ((MV) instr).to;
        } else if (instr instanceof binBase) {
            instr.useNum = 0;
            if (!Objects.equals(((binBase) instr).lhs, "zero")) {
                nowBlock.blockLive.addUse(((binBase) instr).lhs);
                instr.use[instr.useNum++] = ((binBase) instr).lhs;
            }
            if (!Objects.equals(((binBase) instr).rhs, "zero")) {
                nowBlock.blockLive.addUse(((binBase) instr).rhs);
                instr.use[instr.useNum++] = ((binBase) instr).rhs;
            }
            nowBlock.blockLive.addDef(((binBase) instr).to);
            instr.def = ((binBase) instr).to;
        } else if (instr instanceof binImmeBase) {
            instr.useNum = 0;
            nowBlock.blockLive.addUse(((binImmeBase) instr).from);
            nowBlock.blockLive.addDef(((binImmeBase) instr).to);
            instr.use[0] = ((binImmeBase) instr).from;
            instr.def = ((binImmeBase) instr).to;
            instr.useNum = 1;
        } else if (instr instanceof SEQZ) {
            instr.useNum = 0;
            nowBlock.blockLive.addUse(((SEQZ) instr).from);
            nowBlock.blockLive.addDef(((SEQZ) instr).to);
            instr.use[instr.useNum++] = ((SEQZ) instr).from;
            instr.def = ((SEQZ) instr).to;
        } else if (instr instanceof SNEZ) {
            instr.useNum = 0;
            nowBlock.blockLive.addUse(((SNEZ) instr).from);
            nowBlock.blockLive.addDef(((SNEZ) instr).to);
            instr.use[instr.useNum++] = ((SNEZ) instr).from;
            instr.def = ((SNEZ) instr).to;
        } else if (instr instanceof BNEZ) {
            instr.useNum = 0;
            nowBlock.blockLive.addUse(((BNEZ) instr).condition);
            instr.use[instr.useNum++] = ((BNEZ) instr).condition;
        }
    }
}
