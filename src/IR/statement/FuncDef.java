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

    public IRType irType;
    public String functionName;
    public List<IRType> parameterTypeList;
    public List<Block> irList;

    public Stack<ifStatus> ifStatusStack;
    public Stack<loopStatus> loopStatusStack;

    public Stack<Boolean> ifAndLoopOrder;//true为if，反之为loop

    public boolean notReturn = true;
    public boolean isClassMethod = false;
    public int maxCallPara = -1;
    public List<Alloca> allocaList;
    public List<Call> initList;


    public FuncDef() {
        irList = new ArrayList<>();
        parameterTypeList = new ArrayList<>();
        ifStatusStack = new Stack<>();
        loopStatusStack = new Stack<>();
        ifAndLoopOrder = new Stack<>();
        allocaList = new ArrayList<>();
        initList = new ArrayList<>();
    }

    public void pushPara(Type parameterType) {
        parameterTypeList.add(new IRType(parameterType));
    }

    public void pushBlock(Block block) {
        irList.add(block);
    }

    public void push(Instruction instruction) {
        if (instruction instanceof Alloca) {
            allocaList.add((Alloca) instruction);
        } else {
            irList.get(irList.size() - 1).instrList.add(instruction);
        }
        if (instruction instanceof Call) {
            if (((Call) instruction).callTypeList.size() > maxCallPara) {
                maxCallPara = ((Call) instruction).callTypeList.size();
            }
        }
    }

    public int pop() {//用于弹出对赋值号左侧不必要的指令
        Instruction tmp;
        var instrList = irList.get(irList.size() - 1).instrList;
        int minus = 0;//匿名变量编号需要减少的值
        while (true) {
            tmp = instrList.remove(instrList.size() - 1);
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

    public String nowLabel() {
        return "%" + irList.get(irList.size() - 1).label;
    }

}
