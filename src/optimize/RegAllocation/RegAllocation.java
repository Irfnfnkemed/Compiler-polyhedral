package src.optimize.RegAllocation;

import src.ASM.ASMBuilder;
import src.ASM.instruction.*;
import src.ASM.instruction.binaryImme.ADDI;
import src.optimize.Inline.ASMInline.ASMInline;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RegAllocation {
    public HashMap<String, Function> functions;
    public ASMInline ASMInline;

    public RegAllocation(ASMBuilder asmBuilder) throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        ASMInline = new ASMInline();
        functions = new HashMap<>();
        HashMap<String, List<ASMInstr>> ASMInstrMap = new HashMap<>();
        for (var instrList : asmBuilder.asmProgram.sectionText.asmInstrList) {
            ASMInstrMap.put(((LABEL) instrList.get(0)).label, instrList);
        }
        while (!asmBuilder.inlineQueue.isEmpty()) {
            String funcName = asmBuilder.inlineQueue.poll();
            var list = ASMInstrMap.get(funcName);
            ASMInstrMap.remove(funcName);
            Function function = new Function(list, asmBuilder.globalVar);
            if (function.graphColor.used.size() < 15 && function.asmInstrList.size() < 20000) {
                var funcNode = asmBuilder.getNode(funcName);
                ASMInline.changeParaAndRet((Init) list.get(1));
                for (CALL call : funcNode.callList) {
                    ASMInline.inline(call, list);
                }
                for (var fromNode : funcNode.fromNode) {
                    fromNode.toNode.remove(funcNode);
                    if (fromNode.toNode.size() == 0 && !Objects.equals(fromNode.funcName, "main")) {
                        asmBuilder.inlineQueue.add(fromNode.funcName);
                    }
                }
            } else {
                function.allocateReg();
                functions.put(funcName, function);
            }
        }
        for (int i = 0; i < asmBuilder.asmProgram.sectionText.asmInstrList.size(); ++i) {
            var list = asmBuilder.asmProgram.sectionText.asmInstrList.get(i);
            var func = functions.get(((LABEL) list.get(0)).label);
            if (func != null) {
                asmBuilder.asmProgram.sectionText.asmInstrList.set(i, func.asmInstrList);
            }
        }
        for (int i = 0; i < asmBuilder.asmProgram.sectionText.asmInstrList.size(); ++i) {
            var list = asmBuilder.asmProgram.sectionText.asmInstrList.get(i);
            if (ASMInstrMap.containsKey(((LABEL) list.get(0)).label)) {
                Function function = new Function(asmBuilder.asmProgram.sectionText.asmInstrList.get(i), asmBuilder.globalVar);
                function.allocateReg();
                asmBuilder.asmProgram.sectionText.asmInstrList.set(i, function.asmInstrList);
                functions.put(((LABEL) list.get(0)).label, function);
            }
        }
        for (Function function : functions.values()) {
            int max = -1;
            for (var call : function.graphColor.callerRestoreList) {
                var callFunc = functions.get(call.funcName);
                if (callFunc != null && !asmBuilder.funcNodeMap.get(call.funcName).restore) {
                    var iter = call.callerSave.callerReg.iterator();
                    while (iter.hasNext()) {
                        String reg = iter.next();
                        if (!Objects.equals(reg, "t1") && reg.charAt(0) != 'a' &&
                                !call.callerSave.callerReg.contains(reg)) {
                            iter.remove();
                        }
                    }
                }
                int callParaStack = call.callerSave.paraSize > 8 ? call.callerSave.paraSize - 8 : 0;
                if (call.callerSave.callerReg.size() + callParaStack > max) {
                    max = call.callerSave.callerReg.size() + callParaStack;
                }
            }
            if (max > -1) {
                function.call = true;
            } else {
                max = 0;
            }
            function.stackSize = (((function.call ? 1 : 0) + function.savedReg.size() +
                    function.graphColor.stack.size() + max + 3) >> 2) << 4;//+3为了对16字节取整
            Init init = null;
            int tmp = 2 + function.savedReg.size();
            for (int i = 0; i < function.asmInstrList.size(); ++i) {
                var instr = function.asmInstrList.get(i);
                if (instr instanceof LW) {
                    if (Objects.equals(((LW) instr).from, "stack#")) {
                        ((LW) instr).from = "sp";
                        ((LW) instr).offset = function.stackSize - ((tmp + ((LW) instr).offset) << 2);
                    } else if (Objects.equals(((LW) instr).from, "stackTop#")) {
                        ((LW) instr).from = "sp";
                        ((LW) instr).offset = function.stackSize + ((LW) instr).offset;
                    }
                } else if (instr instanceof SW) {
                    if (Objects.equals(((SW) instr).to, "stack#")) {
                        ((SW) instr).to = "sp";
                        ((SW) instr).offset = function.stackSize - ((tmp + ((SW) instr).offset) << 2);
                    } else if (Objects.equals(((SW) instr).to, "stackTop#")) {
                        ((SW) instr).to = "sp";
                        ((SW) instr).offset = function.stackSize + ((SW) instr).offset;
                    }
                } else if (instr instanceof Init) {
                    init = (Init) instr;
                    if (function.stackSize > 0) {
                        init.initList.add(new ADDI("sp", "sp", -function.stackSize));
                    }
                    int initTmp = 1;
                    for (String reg : function.savedReg) {
                        init.initList.add(new SW(reg, "sp", function.stackSize - (initTmp++ << 2)));
                    }
                    if (function.call) {
                        init.initList.add(new SW("ra", "sp", function.stackSize - (initTmp++ << 2)));
                    }
                } else if (instr instanceof Restore) {
                    assert init != null;
                    ((Restore) instr).set(init);
                }
            }
            for (var call : function.graphColor.callerRestoreList) {
                int callTmp = 0, callParaStack = call.callerSave.paraSize > 8 ? call.callerSave.paraSize - 8 : 0;
                for (var reg : call.callerSave.callerReg) {
                    call.callerSave.callerList.add(new SW(reg, "sp", (callParaStack + callTmp) << 2));
                    call.callerList.add(new LW("sp", reg, (callParaStack + callTmp++) << 2));
                }
            }
        }
        for (int i = 0; i < asmBuilder.asmProgram.sectionText.asmInstrList.size(); ++i) {
            var instrList = asmBuilder.asmProgram.sectionText.asmInstrList.get(i);
            if (!functions.containsKey(((LABEL) instrList.get(0)).label)) {
                asmBuilder.asmProgram.sectionText.asmInstrList.remove(i--);
            } else {
                asmBuilder.asmProgram.sectionText.asmInstrList.set(i, mergeADDI(instrList));
            }
        }
    }

    public List<ASMInstr> mergeADDI(List<ASMInstr> asmInstrList) {
        List<ASMInstr> newASMInstrList = new ArrayList<>();
        ASMInstr now = null, pre = null;
        for (ASMInstr asmInstr : asmInstrList) {
            now = asmInstr;
            if (pre instanceof ADDI && now instanceof ADDI && Objects.equals(((ADDI) now).from, ((ADDI) now).to) &&
                    Objects.equals(((ADDI) pre).to, ((ADDI) now).to)) {
                ((ADDI) pre).imme += ((ADDI) now).imme;
            } else {
                newASMInstrList.add(now);
                pre = now;
            }
        }
        return newASMInstrList;
    }
}
