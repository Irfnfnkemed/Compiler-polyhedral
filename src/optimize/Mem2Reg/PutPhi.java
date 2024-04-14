package src.optimize.Mem2Reg;

import src.IR.instruction.*;
import src.IR.statement.FuncDef;
import src.Util.cell.Cell;

import java.util.*;

public class PutPhi {

    public static class variable {
        public Cell var;

        public variable(String varName_) {
            var = new Cell().set(varName_);
        }

        public variable(long varValue_) {
            var = new Cell().set(varValue_);
        }

        public variable(String varName_, long varValue_) {
            if (varName_ == null) {
                var = new Cell().set(varValue_);
            } else {
                var = new Cell().set(varName_);
            }
        }

        public variable(Cell cell) {
            var = new Cell().set(cell);
        }

        public variable(variable variable_) {
            var = new Cell().set(variable_.var);
        }
    }

    public CFGDom cfgDom;
    public Dom dom;
    public HashMap<String, Stack<variable>> varRename;//重命名栈
    public HashMap<String, variable> replace;//替换变量
    public FuncDef funcDef;
    Stack<renameBlockPara> renameBlockStack;//防止爆栈，手动实现

    static class renameBlockPara {
        BlockDom blockDom;
        String fromLabel;
        HashMap<String, variable> restoreStack;
        int i;//i=0，表示第一次入栈；反之，表示递归处理第i个后继

        public renameBlockPara(BlockDom blockDom_, String fromLabel_, HashMap<String, variable> restoreStack_, int i_) {
            blockDom = blockDom_;
            fromLabel = fromLabel_;
            restoreStack = restoreStack_;
            i = i_;
        }
    }

    public PutPhi(Dom dom_, FuncDef funcDef_) {
        cfgDom = dom_.cfgDom;
        dom = dom_;
        funcDef = funcDef_;
        varRename = new HashMap<>();
        cfgDom.allocaVar.keySet().forEach(varName -> varRename.put(varName, new Stack<>()));
        replace = new HashMap<>();
        renameBlockStack = new Stack<>();
        setPhiPos();
        rename();
        recollect();
    }

    public void setPhiPos() {
        HashSet<String> defLabelSet = new HashSet<>();//有def的块
        for (var entry : cfgDom.allocaVar.entrySet()) {
            int index = 0;
            defLabelSet.clear();
            String varName = entry.getKey();
            var defList = entry.getValue();
            defLabelSet.addAll(defList);
            while (index < defList.size()) {
                if (!cfgDom.funcBlockDoms.containsKey(defList.get(index))) {
                    ++index;
                    continue;
                }
                for (String putBlockLabel : dom.domMap.get(defList.get(index)).domFrontier) {
                    if (!(cfgDom.funcBlockDoms.get(putBlockLabel)).insertPhi.containsKey(varName)) {
                        (cfgDom.funcBlockDoms.get(putBlockLabel)).insertPhi.put(
                                varName, new Phi(cfgDom.allocaVarType.get(varName), varName + "-" + putBlockLabel));
                    }
                    if (!defLabelSet.contains(putBlockLabel)) {
                        defList.add(putBlockLabel);
                        defLabelSet.add(putBlockLabel);
                    }
                }
                ++index;
            }
        }
    }

    public void rename() {
        BlockDom root = cfgDom.funcBlockDoms.get("entry");
        renameBlockStack.push(new renameBlockPara(root, null, null, 0));
        while (!renameBlockStack.isEmpty()) {
            var para = renameBlockStack.peek();
            if (para.i == 0) {
                var restoreStack = renameBlock(para.blockDom, para.fromLabel);
                if (para.blockDom.suc > 0 && restoreStack != null) {
                    para.restoreStack = restoreStack;
                    renameBlockStack.push(new renameBlockPara(para.blockDom.next.get(para.i++), para.blockDom.block.label, restoreStack, 0));
                } else {
                    renameBlockStack.pop();
                }
            } else {
                for (var entry : varRename.entrySet()) {//恢复栈
                    var restore = para.restoreStack.get(entry.getKey());
                    var stack = entry.getValue();
                    if (restore != null) {
                        while (restore != stack.peek()) {
                            stack.pop();
                        }
                    } else {
                        stack.clear();
                    }
                }
                if (para.i < para.blockDom.suc) {
                    renameBlockStack.push(new renameBlockPara(para.blockDom.next.get(para.i++), para.blockDom.block.label, null, 0));
                } else {
                    renameBlockStack.pop();
                }
            }
        }

    }

    private HashMap<String, variable> renameBlock(BlockDom blockDom, String fromLabel) {
        for (var entry : blockDom.insertPhi.entrySet()) {//phi变量放置
            if (!varRename.get(entry.getKey()).isEmpty()) {
                boolean newLabel = true;
                var varDef = varRename.get(entry.getKey()).peek();
                for (var assign : entry.getValue().assignBlockList) {
                    if (Objects.equals(assign.label, "%" + fromLabel)) {
                        newLabel = false;
                        break;
                    }
                }
                if (!newLabel) {
                    continue;
                }
                entry.getValue().push(varDef.var, "%" + fromLabel);
                varRename.get(entry.getKey()).push(new variable(entry.getValue().result));
            }
        }
        variable tmpVar;
        for (var inst : blockDom.block.instrList) {
            if (inst instanceof Phi) {
                if (((Phi) inst).assignBlockList.size() > blockDom.pre) {
                    for (int j = 0; j < ((Phi) inst).assignBlockList.size(); ++j) {
                        boolean flag = true;
                        if (!cfgDom.funcBlockDoms.containsKey(((Phi) inst).assignBlockList.get(j).label.substring(1))) {
                            flag = false;
                        } else {
                            for (var preBlock : blockDom.prev) {
                                if (Objects.equals(preBlock.block.label, ((Phi) inst).assignBlockList.get(j).label.substring(1))) {
                                    flag = false;
                                    break;
                                }
                            }
                        }
                        if (flag) {
                            ((Phi) inst).assignBlockList.remove(j--);//移除死块/不会像目标跳转块来的phi赋值
                        }
                    }
                }
                for (var assignBlock : ((Phi) inst).assignBlockList) {
                    if (!assignBlock.variable.isConst) {
                        tmpVar = replace.get(assignBlock.variable.varName);
                        if (tmpVar != null) {
                            assignBlock.variable.set(tmpVar.var);
                        }
                    }
                }
                if (((Phi) inst).assignBlockList.size() == 1) {
                    var assign = ((Phi) inst).assignBlockList.get(0);
                    replace.put(((Phi) inst).result, new variable(assign.variable));
                    ((Phi) inst).result = null;//表明要移除phi
                }
            } else {
                if (blockDom.renamed) {
                    return null;
                }
                if (inst instanceof Binary) {
                    if (!((Binary) inst).left.isConst) {
                        tmpVar = replace.get(((Binary) inst).left.varName);
                        if (tmpVar != null) {
                            ((Binary) inst).left.set(tmpVar.var);
                        }
                    }
                    if (!((Binary) inst).right.isConst) {
                        tmpVar = replace.get(((Binary) inst).right.varName);
                        if (tmpVar != null) {
                            ((Binary) inst).right.set(tmpVar.var);
                        }
                    }
                    if (((Binary) inst).left.isConst && ((Binary) inst).right.isConst) {
                        long out = 0;
                        try {
                            switch (((Binary) inst).op) {
                                case "add" -> out = ((Binary) inst).left.varValue + ((Binary) inst).right.varValue;
                                case "sub" -> out = ((Binary) inst).left.varValue - ((Binary) inst).right.varValue;
                                case "mul" -> out = ((Binary) inst).left.varValue * ((Binary) inst).right.varValue;
                                case "sdiv" -> out = ((Binary) inst).left.varValue / ((Binary) inst).right.varValue;
                                case "srem" -> out = ((Binary) inst).left.varValue % ((Binary) inst).right.varValue;
                                case "shl" -> out = ((Binary) inst).left.varValue << ((Binary) inst).right.varValue;
                                case "ashr" -> out = ((Binary) inst).left.varValue >> ((Binary) inst).right.varValue;
                                case "and" -> out = ((Binary) inst).left.varValue & ((Binary) inst).right.varValue;
                                case "or" -> out = ((Binary) inst).left.varValue | ((Binary) inst).right.varValue;
                                case "xor" -> out = ((Binary) inst).left.varValue ^ ((Binary) inst).right.varValue;
                            }
                            replace.put(((Binary) inst).output, new variable(out));
                            ((Binary) inst).output = null;//表明要删去
                        } catch (Exception exception) {//直接计算会抛出错误(除以0，位移负数等)
                        }
                    }
                } else if (inst instanceof Icmp) {
                    if (!((Icmp) inst).left.isConst) {
                        tmpVar = replace.get(((Icmp) inst).left.varName);
                        if (tmpVar != null) {
                            ((Icmp) inst).left.set(tmpVar.var);
                        }
                    }
                    if (!((Icmp) inst).right.isConst) {
                        tmpVar = replace.get(((Icmp) inst).right.varName);
                        if (tmpVar != null) {
                            ((Icmp) inst).right.set(tmpVar.var);
                        }
                    }
                    if (((Icmp) inst).left.isConst && ((Icmp) inst).right.isConst) {
                        boolean out = false;
                        switch (((Icmp) inst).cond) {
                            case "slt" -> out = ((Icmp) inst).left.varValue < ((Icmp) inst).right.varValue;
                            case "sgt" -> out = ((Icmp) inst).left.varValue > ((Icmp) inst).right.varValue;
                            case "sle" -> out = ((Icmp) inst).left.varValue <= ((Icmp) inst).right.varValue;
                            case "sge" -> out = ((Icmp) inst).left.varValue >= ((Icmp) inst).right.varValue;
                            case "eq" -> out = ((Icmp) inst).left.varValue == ((Icmp) inst).right.varValue;
                            case "ne" -> out = ((Icmp) inst).left.varValue != ((Icmp) inst).right.varValue;
                        }
                        replace.put(((Icmp) inst).output, new variable(out ? 1 : 0));
                        ((Icmp) inst).output = null;//表明要删去
                    }
                } else if (inst instanceof Load) {
                    if (cfgDom.allocaVar.containsKey(((Load) inst).fromPointer)) {
                        replace.put(((Load) inst).toVarName, new variable(varRename.get(((Load) inst).fromPointer).peek()));
                        ((Load) inst).toVarName = null;//表示改指令会在后续删去
                    }
                } else if (inst instanceof Store) {
                    if (!((Store) inst).value.isConst) {
                        tmpVar = replace.get(((Store) inst).value.varName);
                        if (tmpVar != null) {
                            ((Store) inst).value.set(tmpVar.var);
                        }
                    }
                    if (cfgDom.allocaVar.containsKey(((Store) inst).toPointer)) {
                        varRename.get(((Store) inst).toPointer).push(new variable(((Store) inst).value));
                        ((Store) inst).toPointer = null;//表示改指令会在后续删去
                    }
                } else if (inst instanceof Br) {
                    if (((Br) inst).condition != null) {
                        tmpVar = replace.get(((Br) inst).condition);
                        if (tmpVar != null) {
                            if (!tmpVar.var.isConst) {
                                ((Br) inst).condition = tmpVar.var.varName;
                            } else {
                                cfgDom.change = true;//控制流发生改变
                                ((Br) inst).condition = null;
                                if (tmpVar.var.varValue == 0) {
                                    ((Br) inst).trueLabel = ((Br) inst).falseLabel;
                                }
                            }

                        }
                    }
                } else if (inst instanceof Call) {
                    for (var variable : ((Call) inst).callList) {
                        if (variable.varName != null) {
                            tmpVar = replace.get(variable.varName);
                            if (tmpVar != null) {
                                variable.set(tmpVar.var);
                            }
                        }
                    }
                } else if (inst instanceof Getelementptr) {
                    if (!((Getelementptr) inst).index.isConst) {
                        tmpVar = replace.get(((Getelementptr) inst).index.varName);
                        if (tmpVar != null) {
                            ((Getelementptr) inst).index.set(tmpVar.var);
                        }
                    }
                    tmpVar = replace.get(((Getelementptr) inst).from);
                    if (tmpVar != null) {
                        ((Getelementptr) inst).from = tmpVar.var.varName;
                    }
                } else if (inst instanceof Ret) {
                    if (((Ret) inst).retVar != null && !((Ret) inst).retVar.isConst) {
                        tmpVar = replace.get(((Ret) inst).retVar.varName);
                        if (tmpVar != null) {
                            ((Ret) inst).retVar.set(tmpVar.var);
                        }
                    }
                }
            }
        }
        blockDom.renamed = true;
        HashMap<String, variable> restoreStack = new HashMap<>();
        for (var entry : varRename.entrySet()) {
            if (!entry.getValue().empty()) {
                restoreStack.put(entry.getKey(), entry.getValue().peek());
            }
        }
        return restoreStack;
    }

    private void recollect() {
        List<Block> newIrList = new ArrayList<>();
        BlockDom nowBlockDom;
        Instruction instruction;
        for (var block : funcDef.irList) {
            nowBlockDom = cfgDom.funcBlockDoms.get(block.label);
            if (nowBlockDom == null) {
                continue;//移除死块
            }
            newIrList.add(block);
            List<Instruction> newInstrList = new ArrayList<>();
            for (var phi : nowBlockDom.insertPhi.values()) {//放置phi
                if (phi.assignBlockList.size() >= nowBlockDom.pre) {//分支数不够，表明实际上该变量不会在该块中作用；过多，需要去除多余
                    for (int j = 0; j < phi.assignBlockList.size(); ++j) {
                        var assign = phi.assignBlockList.get(j);
                        boolean flag = true;
                        for (var preBlock : nowBlockDom.prev) {//确认phi来源的标签存在
                            if (assign.label.substring(1).equals(preBlock.block.label)) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) {
                            phi.assignBlockList.remove(j--);
                        }
                    }
                    if (phi.assignBlockList.size() == nowBlockDom.pre) {
                        newInstrList.add(phi);//确认分支数正确
                    }
                }
            }
            for (int j = 0; j < nowBlockDom.block.instrList.size(); ++j) {
                instruction = nowBlockDom.block.instrList.get(j);
                if ((instruction instanceof Load && ((Load) instruction).toVarName == null) ||
                        (instruction instanceof Store && ((Store) instruction).toPointer == null) ||
                        (instruction instanceof Binary && ((Binary) instruction).output == null) ||
                        (instruction instanceof Icmp && ((Icmp) instruction).output == null) ||
                        (instruction instanceof Phi && ((Phi) instruction).result == null) ||
                        (instruction instanceof Alloca)) {
                    continue;
                }
                if (instruction instanceof Phi) {
                    if (((Phi) instruction).assignBlockList.size() >= nowBlockDom.pre) {//分支数不够，表明实际上该变量不会在该块中作用；过多，需要去除多余
                        for (int k = 0; k < ((Phi) instruction).assignBlockList.size(); ++k) {
                            var assign = ((Phi) instruction).assignBlockList.get(k);
                            boolean flag = true;
                            for (var preBlock : nowBlockDom.prev) {//确认phi来源的标签存在
                                if (assign.label.substring(1).equals(preBlock.block.label)) {
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                ((Phi) instruction).assignBlockList.remove(k--);
                            }
                        }
                        if (((Phi) instruction).assignBlockList.size() != nowBlockDom.pre) {
                            continue;
                        }
                    }
                }
                newInstrList.add(instruction);
            }
            block.instrList = newInstrList;
        }
        funcDef.irList = newIrList;
    }
}
