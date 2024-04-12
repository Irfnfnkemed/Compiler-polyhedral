package src.IR.statement;

import src.IR.instruction.*;
import src.Util.type.IRType;
import src.Util.type.Type;

import java.util.*;

public class FuncDef extends IRStatement {
    public static class ifStatus {
        public boolean trueJump = false;
        public boolean trueNotReturn = true;
        public boolean falseJump = false;
        public boolean falseNotReturn = true;
        public boolean onTrue = true;
    }

    public static class loopStatus {
        public boolean jump = false;
    }

    public static class PhiInfo {//用于处理关于phi的变量

        public static class PhiBlock {
            public String fromVar, toVar;
            public long value;

            public PhiBlock(String fromVar_, String toVar_, long value_) {
                fromVar = fromVar_;
                toVar = toVar_;
                value = value_;
            }
        }

        public List<PhiBlock> phiTrueList;//节点到跳转目标块(true跳转)，所有需要赋值的phi
        public List<PhiBlock> phiFalseList;//节点到跳转目标块(false跳转)，所有需要赋值的phi
        public Br br;

        public PhiInfo(Br br_) {
            phiTrueList = new ArrayList<>();
            phiFalseList = new ArrayList<>();
            br = br_;
        }

        public void push(String fromVar_, String toVar_, long value_, String label_) {
            if (Objects.equals(label_, br.trueLabel.substring(1))) {
                phiTrueList.add(new PhiBlock(fromVar_, toVar_, value_));
            } else {
                phiFalseList.add(new PhiBlock(fromVar_, toVar_, value_));
            }
        }
    }

    public IRType irType;
    public String functionName;
    public List<IRType> parameterTypeList;
    public List<Instruction> irList;
    public List<Label> labelList;

    public Stack<ifStatus> ifStatusStack;
    public Stack<loopStatus> loopStatusStack;

    public Stack<Boolean> ifAndLoopOrder;//true为if，反之为loop

    public boolean notReturn = true;
    public int initInsertIndex = 1;
    public int allocaIndex = 1;
    public String label = "%entry";
    public String returnLabel = "returnLabel";
    public boolean isClassMethod = false;
    public int maxCallPara = -1;
    public HashMap<String, PhiInfo> phiMap;//phi指令，跳转来源标签->目标标签及赋值语段，便于汇编处理
    public List<Alloca> allocaList;


    public FuncDef() {
        irList = new ArrayList<>();
        parameterTypeList = new ArrayList<>();
        ifStatusStack = new Stack<>();
        loopStatusStack = new Stack<>();
        ifAndLoopOrder = new Stack<>();
        phiMap = new HashMap<>();
        labelList = new ArrayList<>();
        allocaList = new ArrayList<>();
    }

    public void pushPara(Type parameterType) {
        parameterTypeList.add(new IRType(parameterType));
    }

    public void push(Instruction instruction) {
        if (instruction instanceof Alloca) {
            allocaList.add((Alloca) instruction);
        } else {
            irList.add(instruction);
        }
        if (instruction instanceof Label) {
            label = "%" + ((Label) instruction).labelName;
            labelList.add((Label) instruction);
        }
        if (instruction instanceof Call) {
            if (((Call) instruction).callTypeList.size() > maxCallPara) {
                maxCallPara = ((Call) instruction).callTypeList.size();
            }
        }
    }

    public void insertAlloca() {
        for (var alloca : allocaList) {
            irList.add(allocaIndex++, alloca);
        }
    }

    public int pop() {//用于弹出对赋值号左侧不必要的指令
        Instruction tmp;
        int tail = irList.size() - 1;
        int minus = 0;//匿名变量编号需要减少的值
        while (true) {
            tmp = irList.get(tail);
            irList.remove(tail--);
            if (tmp instanceof Binary) {
                ++minus;
            } else if (tmp instanceof Load) {
                ++minus;
                break;
            }
        }
        return minus;
    }

    public void pushIf() {
        ifStatusStack.push(new ifStatus());
        ifAndLoopOrder.push(true);
    }

    public void pushLoop() {
        loopStatusStack.push(new loopStatus());
        ifAndLoopOrder.push(false);
    }

    public ifStatus getIf() {
        if (ifStatusStack.size() != 0) {
            return ifStatusStack.peek();
        }
        return null;
    }

    public loopStatus getLoop() {
        if (loopStatusStack.size() != 0) {
            return loopStatusStack.peek();
        }
        return null;
    }

    public void popIf() {
        ifStatusStack.pop();
        ifAndLoopOrder.pop();
    }

    public void popLoop() {
        loopStatusStack.pop();
        ifAndLoopOrder.pop();
    }

    public boolean isIf() {
        return ifAndLoopOrder.peek();
    }

    public void collectPhi() {
        String nowLabel = null;
        Stack<String> labelStack = new Stack<>();
        Stack<Phi> phiStack = new Stack<>();
        for (var instr : irList) {
            if (instr instanceof Label) {
                nowLabel = ((Label) instr).labelName;
            } else if (instr instanceof Br) {
                ((Br) instr).nowLabel = "%" + nowLabel;
                phiMap.put(nowLabel, new PhiInfo((Br) instr));
            } else if (instr instanceof Phi) {
                labelStack.push(nowLabel);
                phiStack.push((Phi) instr);
            }
        }
        while (!phiStack.isEmpty()) {
            var phi = phiStack.pop();
            label = labelStack.pop();
            for (var assignBlock : phi.assignBlockList) {
                PhiInfo phiInfo = phiMap.get(assignBlock.label.substring(1));
                if (phiInfo != null) {
                    phiInfo.push(assignBlock.variable.varName, phi.result, assignBlock.variable.varValue, label);
                }
            }
        }
    }

}
