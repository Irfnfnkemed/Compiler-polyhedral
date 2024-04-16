package src.optimize.RegAllocation;

import src.ASM.instruction.*;
import src.ASM.instruction.binary.binBase;
import src.ASM.instruction.binaryImme.binImmeBase;

import java.util.*;

public class GraphColor {
    final int k = 27;
    final String[] reg = {
            "t0", "t2", "t3", "t4", "t5", "t6", "t1",
            "a7", "a6", "a5", "a4", "a3", "a2", "a1", "a0",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    };

    final HashMap<String, Integer> regId = new HashMap<>() {{
        put("t0", 1);
        put("t2", 1 << 1);
        put("t3", 1 << 2);
        put("t4", 1 << 3);
        put("t5", 1 << 4);
        put("t6", 1 << 5);
        put("t1", 1 << 6);
        put("a7", 1 << 7);
        put("a6", 1 << 8);
        put("a5", 1 << 9);
        put("a4", 1 << 10);
        put("a3", 1 << 11);
        put("a2", 1 << 12);
        put("a1", 1 << 13);
        put("a0", 1 << 14);
        put("s0", 1 << 15);
        put("s1", 1 << 16);
        put("s2", 1 << 17);
        put("s3", 1 << 18);
        put("s4", 1 << 19);
        put("s5", 1 << 20);
        put("s6", 1 << 21);
        put("s7", 1 << 22);
        put("s8", 1 << 23);
        put("s9", 1 << 24);
        put("s10", 1 << 25);
        put("s11", 1 << 26);
        put("stackTop#", 0);
        put("sp", 0);
    }};


    public RIG rig;
    public Stack<RIG.RIGNode> selectStack;
    public HashSet<String> spillSet;
    public TreeSet<RIG.RIGNode> simplifyList;
    public TreeSet<RIG.RIGNode> moveList;
    public HashMap<String, String> coalesceMap;
    public HashSet<String> globalVar;
    public HashMap<String, Integer> stack;//栈上变量
    public HashSet<String> used;
    public int cnt;
    public List<CallerRestore> callerRestoreList;//用于最后确定call

    public GraphColor(RIG rig_, HashSet<String> globalVar_, HashMap<String, Integer> stack_, int cnt_) {
        rig = rig_;
        selectStack = new Stack<>();
        spillSet = new HashSet<>();
        simplifyList = new TreeSet<>();
        moveList = new TreeSet<>();
        coalesceMap = new HashMap<>();
        globalVar = globalVar_;
        stack = stack_;
        used = new HashSet<>();
        cnt = cnt_;
        init(rig.rigNodes);
        modifyGraph();
        assignColor();
    }

    public void init(HashMap<String, RIG.RIGNode> nodes) {
        selectStack.clear();
        spillSet.clear();
        simplifyList.clear();
        moveList.clear();
        coalesceMap.clear();
        for (var node : nodes.values()) {
            node.colour = -1;
            if (node.preColored == null) {
                if (node.aboutMove) {
                    moveList.add(node);
                } else {
                    simplifyList.add(node);
                }
            }
        }
    }

    public void modifyGraph() {
        while (!(simplifyList.isEmpty() && moveList.isEmpty())) {
            if (!coalesce()) {
                if (!simplify()) {
                    if (!freeze()) {
                        spilt();
                    }
                }
            }
        }
    }

    public void assignColor() {
        RIG.RIGNode nowNode;
        while (!selectStack.isEmpty()) {
            nowNode = selectStack.pop();
            if (!setColor(nowNode)) {
                spillSet.add(nowNode.varName);
            }
        }
    }

    public boolean allocateReg() {
        if (spillSet.isEmpty()) {
            replace();
            used.remove("stackTop#");
            return true;
        } else {
            rewrite();
            used.remove("stackTop#");
            return false;
        }
    }

    public boolean simplify() {
        if (simplifyList.isEmpty()) {
            return false;
        }
        var node = simplifyList.first();
        if (node.toNode.size() >= k) {//无法简化
            return false;
        }
        remove(node);
        return true;
    }

    public boolean coalesce() {
        if (moveList.isEmpty()) {
            return false;
        }
        var nowNode = moveList.first();
        if (nowNode.preColored != null) {
            return false;
        }
        for (var mvNode : nowNode.mvNode.values()) {
            if (judgeGeorge(nowNode, mvNode)) {
                coalesceMap.put(nowNode.varName, mvNode.varName);
                moveList.remove(nowNode);
                moveList.remove(mvNode);
                for (var toNode : nowNode.toNode.values()) {//合并边
                    if (mvNode != toNode) {
                        mvNode.toNode.put(toNode.varName, toNode);
                    }
                    if (toNode.toNode.containsKey(mvNode.varName)) {
                        if (toNode.preColored != null) {
                            toNode.toNode.remove(nowNode.varName);
                        } else {
                            if (toNode.aboutMove) {
                                moveList.remove(toNode);
                                toNode.toNode.remove(nowNode.varName);
                                moveList.add(toNode);
                            } else {
                                simplifyList.remove(toNode);
                                toNode.toNode.remove(nowNode.varName);
                                simplifyList.add(toNode);
                            }
                        }
                    } else {
                        if (toNode.preColored == null) {
                            if (toNode.aboutMove) {
                                moveList.remove(toNode);
                            } else {
                                simplifyList.remove(toNode);
                            }
                        }
                        toNode.toNode.remove(nowNode.varName);
                        toNode.toNode.put(mvNode.varName, mvNode);
                        if (toNode.preColored == null) {
                            if (toNode.aboutMove) {
                                moveList.add(toNode);
                            } else {
                                simplifyList.add(toNode);
                            }
                        }
                        if (toNode.mvNode.containsKey(mvNode.varName)) {
                            toNode.mvNode.remove(mvNode.varName);
                            mvNode.mvNode.remove(toNode.varName);
                            if (toNode.mvNode.size() == 0) {
                                toNode.aboutMove = false;//不再传送相关
                                if (toNode.preColored == null) {
                                    moveList.remove(toNode);
                                    simplifyList.add(toNode);
                                }
                            }
                        }
                    }
                }
                for (var nowMvNode : nowNode.mvNode.values()) {//更改合并对象
                    nowMvNode.mvNode.remove(nowNode.varName);//不再传送
                    if (nowMvNode != mvNode && !nowMvNode.toNode.containsKey(mvNode.varName)) {
                        nowMvNode.mvNode.put(mvNode.varName, mvNode);
                        mvNode.mvNode.put(nowMvNode.varName, nowMvNode);
                    }
                    if (nowMvNode != mvNode && nowMvNode.mvNode.size() == 0) {
                        nowMvNode.aboutMove = false;//不再传送相关
                        if (nowMvNode.preColored == null) {
                            moveList.remove(nowMvNode);
                            simplifyList.add(nowMvNode);
                        }
                    }
                }
                mvNode.aboutMove = mvNode.mvNode.size() > 0;
                if (mvNode.preColored == null) {
                    if (mvNode.aboutMove) {
                        moveList.add(mvNode);
                    } else {
                        simplifyList.add(mvNode);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean freeze() {
        var nowNode = moveList.pollFirst();
        if (nowNode == null) {
            return false;
        }
        for (var mvNode : nowNode.mvNode.values()) {//放弃传送
            mvNode.mvNode.remove(nowNode.varName);
            if (mvNode.mvNode.size() == 0) {//不再传送相关
                mvNode.aboutMove = false;
                if (mvNode.preColored == null) {
                    moveList.remove(mvNode);
                    simplifyList.add(mvNode);
                }
            }
        }
        nowNode.mvNode.clear();
        nowNode.aboutMove = false;
        simplifyList.add(nowNode);
        return true;
    }

    public void spilt() {
        RIG.RIGNode node = null;
        var descendingSet = simplifyList.descendingSet();
        for (var ele : descendingSet) {
            if (!Objects.equals(ele.varName, "this")) {
                node = ele;
                break;
            }
        }
        if (node == null) {
            return;
        }
        remove(node);
    }

    public void remove(RIG.RIGNode node) {
        selectStack.push(node);
        simplifyList.remove(node);
        for (var toNode : node.toNode.values()) {//移除边，重新排序
            if (toNode.preColored != null) {
                toNode.toNode.remove(node.varName);
            } else {
                if (toNode.aboutMove) {
                    moveList.remove(toNode);
                    toNode.toNode.remove(node.varName);
                    moveList.add(toNode);
                } else {
                    simplifyList.remove(toNode);
                    toNode.toNode.remove(node.varName);
                    simplifyList.add(toNode);
                }
            }
        }
        for (var mvNode : node.mvNode.values()) {
            mvNode.mvNode.remove(node.varName);
            if (mvNode.mvNode.size() == 0) {//不再传送相关
                mvNode.aboutMove = false;
                if (mvNode.preColored == null) {
                    moveList.remove(mvNode);
                    simplifyList.add(mvNode);
                }
            }
        }
    }

    public boolean judgeGeorge(RIG.RIGNode fromNode, RIG.RIGNode mvNode) {
        for (var toNode : fromNode.toNode.values()) {
            if (toNode.toNode.size() >= k && !mvNode.toNode.containsKey(toNode.varName)) {
                return false;
            }
        }
        if (mvNode.preColored != null) {//合并目标已经预染色
            for (var toNode : fromNode.toNode.values()) {
                if (toNode.preColored != null && toNode.preColored.equals(mvNode.preColored)) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean setColor(RIG.RIGNode rigNode) {
        if (rigNode.preColored != null) {
            return true;
        }
        int tmp = 0;
        for (var node : rigNode.toNode.values()) {
            if (coalesceMap.containsKey(node.varName)) {
                node = rig.rigNodes.get(find(node.varName));
            }
            if (node.colour == -1) {
                if (node.preColored != null) {
                    tmp = tmp | regId.get(node.preColored);
                }
            } else {
                tmp = tmp | (1 << node.colour);
            }
        }
        if (rigNode.call) {
            for (int i = 15; i < k; ++i) {
                if ((tmp & (1 << i)) == 0) {
                    rigNode.colour = i;
                    return true;
                }
            }
            for (int i = 0; i < 15; ++i) {
                if ((tmp & (1 << i)) == 0) {
                    rigNode.colour = i;
                    return true;
                }
            }
        } else {
            for (int i = 0; i < k; ++i) {
                if ((tmp & 1) == 0) {
                    rigNode.colour = i;
                    return true;
                }
                tmp = tmp >> 1;
            }
        }
        return false;
    }


    public String find(String varName) {
        String faName = coalesceMap.get(varName);
        if (faName == null) {
            return varName;
        }
        String findName = find(faName);
        coalesceMap.put(varName, findName);
        return findName;
    }

    public String getReg(String varName) {
        if (Objects.equals(varName, "zero")) {
            return varName;
        }
        var node = rig.rigNodes.get(varName);
        if (node == null) {
            if (globalVar.contains(varName) || varName.contains("#")) {
                return varName;
            } else {
                return null;
            }
        }
        if (node.preColored != null) {
            used.add(node.preColored);
            return node.preColored;
        }
        if (node.colour == -1) {
            node = rig.rigNodes.get(find(node.varName));
        }
        if (node.colour == -1) {
            used.add(node.preColored);
            return node.preColored;//预着色点
        }
        used.add(reg[node.colour]);
        return reg[node.colour];
    }

    public void replace() {
        callerRestoreList = new ArrayList<>();
        List<ASMInstr> newList = new ArrayList<>();
        for (int i = 0; i < rig.cfgReg.asmInstrList.size(); ++i) {
            var instr = rig.cfgReg.asmInstrList.get(i);
            if (instr instanceof LI) {
                ((LI) instr).to = getReg(((LI) instr).to);
                if (((LI) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof LW) {
                ((LW) instr).from = getReg(((LW) instr).from);
                ((LW) instr).to = getReg(((LW) instr).to);
                if (((LW) instr).from != null && ((LW) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof LA) {
                ((LA) instr).to = getReg(((LA) instr).to);
                if (((LA) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof SW) {
                ((SW) instr).from = getReg(((SW) instr).from);
                ((SW) instr).to = getReg(((SW) instr).to);
                if (((SW) instr).from != null && ((SW) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof MV) {
                ((MV) instr).from = getReg(((MV) instr).from);
                ((MV) instr).to = getReg(((MV) instr).to);
                if (((MV) instr).from != null && ((MV) instr).to != null && !Objects.equals(((MV) instr).from, ((MV) instr).to)) {
                    newList.add(instr);
                }
            } else if (instr instanceof binBase) {
                ((binBase) instr).lhs = getReg(((binBase) instr).lhs);
                ((binBase) instr).rhs = getReg(((binBase) instr).rhs);
                ((binBase) instr).to = getReg(((binBase) instr).to);
                if (((binBase) instr).lhs != null && ((binBase) instr).rhs != null && ((binBase) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof binImmeBase) {
                ((binImmeBase) instr).from = getReg(((binImmeBase) instr).from);
                ((binImmeBase) instr).to = getReg(((binImmeBase) instr).to);
                if (((binImmeBase) instr).from != null && ((binImmeBase) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof SEQZ) {
                ((SEQZ) instr).from = getReg(((SEQZ) instr).from);
                ((SEQZ) instr).to = getReg(((SEQZ) instr).to);
                if (((SEQZ) instr).from != null && ((SEQZ) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof SNEZ) {
                ((SNEZ) instr).from = getReg(((SNEZ) instr).from);
                ((SNEZ) instr).to = getReg(((SNEZ) instr).to);
                if (((SNEZ) instr).from != null && ((SNEZ) instr).to != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof BNEZ) {
                ((BNEZ) instr).condition = getReg(((BNEZ) instr).condition);
                if (((BNEZ) instr).condition != null) {
                    newList.add(instr);
                }
            } else if (instr instanceof CallerSave) {
                for (String varName : ((CallerSave) instr).varName) {
                    String reg = getReg(varName);
                    if (reg != null) {
                        ((CallerSave) instr).setCallerReg(reg);
                    }
                }
                newList.add(instr);
            } else if (instr instanceof CallerRestore) {
                callerRestoreList.add((CallerRestore) instr);
                newList.add(instr);
            } else {
                newList.add(instr);
            }
        }
        rig.cfgReg.asmInstrList = newList;
    }

    public void rewrite() {
        for (String var : spillSet) {
            stack.put(var, stack.size());
        }
        List<ASMInstr> newList = new ArrayList<>();
        for (int i = 0; i < rig.cfgReg.asmInstrList.size(); ++i) {
            var instr = rig.cfgReg.asmInstrList.get(i);
            if (instr instanceof LI) {
                Integer from = stack.get(((LI) instr).to);
                newList.add(instr);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", from));
                    ((LI) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof LW) {
                Integer from = stack.get(((LW) instr).from), to = stack.get(((LW) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((LW) instr).from = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((LW) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof SW) {
                Integer from = stack.get(((SW) instr).from), to = stack.get(((SW) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((SW) instr).from = "cnt" + cnt++;
                }
                if (to != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, to));
                    ((SW) instr).to = "cnt" + cnt++;
                }
                newList.add(instr);
            } else if (instr instanceof MV) {
                Integer from = stack.get(((MV) instr).from), to = stack.get(((MV) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((MV) instr).from = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((MV) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof binBase) {
                Integer lhs = stack.get(((binBase) instr).lhs), rhs = stack.get(((binBase) instr).rhs),
                        to = stack.get(((binBase) instr).to);
                if (lhs != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, lhs));
                    ((binBase) instr).lhs = "cnt" + cnt++;
                }
                if (rhs != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, rhs));
                    ((binBase) instr).rhs = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((binBase) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof binImmeBase) {
                Integer from = stack.get(((binImmeBase) instr).from), to = stack.get(((binImmeBase) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((binImmeBase) instr).from = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((binImmeBase) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof SEQZ) {
                Integer from = stack.get(((SEQZ) instr).from), to = stack.get(((SEQZ) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((SEQZ) instr).from = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((SEQZ) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof SNEZ) {
                Integer from = stack.get(((SNEZ) instr).from), to = stack.get(((SNEZ) instr).to);
                if (from != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, from));
                    ((SNEZ) instr).from = "cnt" + cnt++;
                }
                newList.add(instr);
                if (to != null) {
                    instr.visited = false;
                    newList.add(new SW("cnt" + cnt, "stack#", to));
                    ((SNEZ) instr).to = "cnt" + cnt++;
                }
            } else if (instr instanceof BNEZ) {
                Integer condition = stack.get(((BNEZ) instr).condition);
                if (condition != null) {
                    instr.visited = false;
                    newList.add(new LW("stack#", "cnt" + cnt, condition));
                    ((BNEZ) instr).condition = "cnt" + cnt++;
                }
                newList.add(instr);
            } else {
                newList.add(instr);
            }
        }
        rig.cfgReg.asmInstrList = newList;
    }
}
