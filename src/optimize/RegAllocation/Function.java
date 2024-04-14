package src.optimize.RegAllocation;

import src.ASM.instruction.ASMInstr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Function {
    public List<ASMInstr> asmInstrList;
    public HashSet<String> savedReg;
    public HashSet<String> unsavedReg;
    public CFGReg cfgReg;
    public GraphColor graphColor;
    public RIG rig;
    public int stackSize;
    public boolean call = false;//是否调用函数
    HashMap<String, Integer> stack;
    public int cnt = 0;

    public Function(List<ASMInstr> asmInstrList_, HashSet<String> globalVar_) {
        asmInstrList = asmInstrList_;
        stack = new HashMap<>();
        cfgReg = new CFGReg(asmInstrList, globalVar_);
        rig = new RIG(cfgReg);
        graphColor = new GraphColor(rig, cfgReg.globalVar, stack, cnt);
    }

    public void allocateReg() {
        while (!graphColor.allocateReg()) {
            asmInstrList = cfgReg.asmInstrList;
            cnt = graphColor.cnt++;
            cfgReg.reset(graphColor.spillSet);//重置
            rig = new RIG(cfgReg);
            graphColor = new GraphColor(rig, cfgReg.globalVar, stack, cnt);
        }
        asmInstrList = cfgReg.asmInstrList;
        savedReg = new HashSet<>();
        unsavedReg = new HashSet<>();
        for (String reg : graphColor.used) {
            if (reg.charAt(0) == 's') {
                savedReg.add(reg);
            } else {
                unsavedReg.add(reg);
            }
        }
    }
}
