package src.optimize.RegAllocation;

import src.ASM.instruction.*;

import java.util.HashMap;
import java.util.HashSet;
public class RIG {
    public static class RIGNode implements Comparable<RIGNode> {
        public HashMap<String, RIGNode> toNode;//无向边连接的对象
        public HashMap<String, RIGNode> mvNode;//传送有关指令
        public String varName;
        public int colour = -1;
        public boolean aboutMove = false;//是否是传送相关的(可能后续被冻结)
        public String preColored;//预着色
        public boolean call = false;//在存活期间是否有函数调用

        public RIGNode(String varName_) {
            toNode = new HashMap<>();
            mvNode = new HashMap<>();
            varName = varName_;
        }

        public int compareTo(RIGNode obj) {
            if (toNode.size() < obj.toNode.size()) {
                return -1;
            } else if (toNode.size() > obj.toNode.size()) {
                return 1;
            } else return varName.compareTo(obj.varName);
        }
    }

    public CFGReg cfgReg;
    public HashMap<String, RIGNode> rigNodes;
    public HashSet<String> removeList;//移除

    public RIG(CFGReg cfgReg_) {
        cfgReg = cfgReg_;
        rigNodes = new HashMap<>();
        removeList = new HashSet<>();
        buildRIG();
    }

    public void buildRIG() {
        ASMInstr asmInstr;
        RIGNode rigNodeNow, rigNodeTo;
        for (var block : cfgReg.blocks.values()) {
            for (int i = block.instructionList.size() - 1; i >= 0; --i) {
                asmInstr = block.instructionList.get(i);
                if (asmInstr instanceof CallerSave) {
                    ((CallerSave) asmInstr).varName.addAll(block.blockLive.liveOut);
                } else {
                    boolean remove = false;
                    if (asmInstr.def != null && !asmInstr.notRemove && asmInstr.preColoredFrom == null &&
                            asmInstr.preColoredTo == null && !block.blockLive.liveOut.contains(asmInstr.def)) {
                        removeList.add(asmInstr.def);
                        var node = rigNodes.get(asmInstr.def);
                        if (node != null) {
                            for (var toNode : node.toNode.values()) {//移除边，重新排序
                                toNode.toNode.remove(node.varName);
                            }
                            for (var mvNode : node.mvNode.values()) {
                                mvNode.mvNode.remove(node.varName);
                            }
                            rigNodes.remove(asmInstr.def);
                        }
                        remove = true;
                    } else {
                        for (int j = 0; j < asmInstr.useNum; ++j) {
                            if (removeList.contains(asmInstr.use[j])) {
                                remove = true;
                                break;
                            }
                        }
                    }
                    if (remove) {
                        continue;
                    }
                    if (asmInstr.def != null) {
                        block.blockLive.liveOut.remove(asmInstr.def);//此处破坏了liveOut，因为后续不会再用到
                        rigNodeNow = getNode(asmInstr.def);
                        if (asmInstr.preColoredTo != null) {
                            rigNodeNow.preColored = asmInstr.preColoredTo;
                        }
                        boolean flag = asmInstr instanceof MV;
                        for (var liveVar : block.blockLive.liveOut) {
                            rigNodeTo = getNode(liveVar);
                            if (rigNodeTo.mvNode.containsKey(asmInstr.def)) {
                                rigNodeTo.mvNode.remove(asmInstr.def);
                                rigNodeNow.mvNode.remove(liveVar);
                            }
                            rigNodeTo.toNode.put(asmInstr.def, rigNodeNow);
                            rigNodeNow.toNode.put(liveVar, rigNodeTo);
                        }
                        if (flag && asmInstr.use[0] != null && !rigNodeNow.toNode.containsKey(asmInstr.use[0])) {
                            rigNodeTo = getNode(asmInstr.use[0]);
                            rigNodeTo.mvNode.put(asmInstr.def, rigNodeNow);
                            rigNodeNow.mvNode.put(asmInstr.use[0], rigNodeTo);
                            rigNodeTo.aboutMove = true;
                            rigNodeNow.aboutMove = true;
                        }
                    }
                    if (asmInstr.use[0] != null) {
                        rigNodeNow = getNode(asmInstr.use[0]);
                        if (asmInstr.preColoredFrom != null) {
                            rigNodeNow.preColored = asmInstr.preColoredFrom;
                        }
                    }
                    if (asmInstr instanceof SW && asmInstr.use[1] != null) {
                        rigNodeNow = getNode(asmInstr.use[1]);
                        if (asmInstr.preColoredTo != null) {
                            rigNodeNow.preColored = asmInstr.preColoredTo;
                        }
                    }
                    for (int j = 0; j < asmInstr.useNum; ++j) {
                        if (getNode(asmInstr.use[j]).preColored == null ||
                                getNode(asmInstr.use[j]).preColored.charAt(0) == 'a') {
                            block.blockLive.liveOut.add(asmInstr.use[j]);
                        }
                    }
                    if (asmInstr instanceof CALL) {
                        block.blockLive.liveOut.forEach(node -> getNode(node).call = true);
                        block.blockLive.liveOut.addAll(((CALL) asmInstr).useList);
                    }
                }
            }
        }
    }

    public RIGNode getNode(String varName) {
        RIGNode rigNode = rigNodes.get(varName);
        if (rigNode == null) {
            rigNode = new RIGNode(varName);
            rigNodes.put(varName, rigNode);
        }
        return rigNode;
    }
}
