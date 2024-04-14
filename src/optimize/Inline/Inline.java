package src.optimize.Inline;

import src.ASM.instruction.*;
import src.ASM.instruction.binary.binBase;
import src.ASM.instruction.binaryImme.binImmeBase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Inline {
    int inline = 0;
    String postFix;
    HashMap<String, String> replace;
    List<String> callPara;

    public Inline() {
        replace = new HashMap<>();
        callPara = new ArrayList<>();
    }

    public void inline(CALL call, List<ASMInstr> list)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {//将函数作为内联函数嵌入
        postFix = "-inline-" + inline++;
        replace.clear();
        CallerSave callerSave = null;
        for (int i = 0; i < call.paraList.size(); ++i) {
            var instr = call.paraList.get(i);
            if (instr instanceof LI) {
                replace.put(callPara.get(i), ((LI) instr).to);
            } else if (instr instanceof MV) {
                instr.ignore = true;
                replace.put(callPara.get(i), ((MV) instr).from);
            } else if (instr instanceof SW) {
                instr.ignore = true;
                replace.put(callPara.get(i), ((SW) instr).from);
            }
            instr.preColoredTo = null;//取消预着色
        }
        if (call.retMV != null) {
            replace.put("ret$", call.retMV.to);
            call.retMV.ignore = true;
            call.retMV.preColoredFrom = null;//取消预着色
        }
        boolean addLabel = false;
        for (int i = 2; i < list.size(); ++i) {
            boolean flagTo = true;
            var instr = list.get(i);
            if (instr.ignore) {
                continue;
            }
            if (instr instanceof LABEL) {
                call.inlineCache.add(new LABEL(((LABEL) instr).label + postFix));
                continue;
            } else if (instr instanceof LI) {
                call.inlineCache.add(new LI(getInlineVarName(((LI) instr).to), ((LI) instr).imme));
            } else if (instr instanceof LW) {
                if (((LW) instr).offset == -2) {
                    call.inlineCache.add(new MV(getInlineVarName(((LW) instr).from), getInlineVarName(((LW) instr).to)));
                } else if (((LW) instr).offset == -1) {
                    call.inlineCache.add(new LW(((LW) instr).from, getInlineVarName(((LW) instr).to), ((LW) instr).offset));
                } else {
                    call.inlineCache.add(new LW(getInlineVarName(((LW) instr).from), getInlineVarName(((LW) instr).to), ((LW) instr).offset));
                }
            } else if (instr instanceof LA) {
                call.inlineCache.add(new LA(getInlineVarName(((LA) instr).to), ((LA) instr).fromLabel));
            } else if (instr instanceof SW) {
                call.inlineCache.add(new SW(getInlineVarName(((SW) instr).from), getInlineVarName(((SW) instr).to), ((SW) instr).offset));
            } else if (instr instanceof MV) {
                if (((MV) instr).to.contains("$")) {
                    flagTo = false;
                }
                call.inlineCache.add(new MV(getInlineVarName(((MV) instr).from), getInlineVarName(((MV) instr).to)));
            } else if (instr instanceof binBase) {
                Class<?> clazz = instr.getClass();
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, String.class);
                constructor.setAccessible(true); // 设置为可访问
                Object newObj = constructor.newInstance(getInlineVarName(((binBase) instr).lhs),
                        getInlineVarName(((binBase) instr).rhs), getInlineVarName(((binBase) instr).to));
                call.inlineCache.add((ASMInstr) newObj);
            } else if (instr instanceof binImmeBase) {
                Class<?> clazz = instr.getClass();
                Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, int.class);
                constructor.setAccessible(true); // 设置为可访问
                Object newObj = constructor.newInstance(getInlineVarName(((binImmeBase) instr).to),
                        getInlineVarName(((binImmeBase) instr).from), ((binImmeBase) instr).imme);
                call.inlineCache.add((ASMInstr) newObj);
            } else if (instr instanceof SEQZ) {
                call.inlineCache.add(new SEQZ(getInlineVarName(((SEQZ) instr).from), getInlineVarName(((SEQZ) instr).to)));
            } else if (instr instanceof SNEZ) {
                call.inlineCache.add(new SNEZ(getInlineVarName(((SNEZ) instr).from), getInlineVarName(((SNEZ) instr).to)));
            } else if (instr instanceof BNEZ) {
                call.inlineCache.add(new BNEZ(getInlineVarName(((BNEZ) instr).condition), ((BNEZ) instr).toLabel + postFix));
            } else if (instr instanceof CALL) {
                CALL ASMcall = new CALL(((CALL) instr).func);
                ASMcall.def = getInlineVarName(instr.def);
                call.inlineCache.add(ASMcall);
            } else if (instr instanceof CallerSave) {
                callerSave = new CallerSave(((CallerSave) instr).paraSize);
                call.inlineCache.add(callerSave);
            } else if (instr instanceof CallerRestore) {
                call.inlineCache.add(new CallerRestore(callerSave, ((CallerRestore) instr).funcName));
            } else if (instr instanceof J) {
                call.inlineCache.add(new J(((J) instr).toLabel + postFix));
            } else if (instr instanceof RET) {
                if (i != list.size() - 1) {
                    addLabel = true;
                    call.inlineCache.add(new J("ret" + postFix));
                }
            } else {
                continue;
            }
            var inlineInstr = call.inlineCache.get(call.inlineCache.size() - 1);
            inlineInstr.preColoredFrom = instr.preColoredFrom;
            if (flagTo) {
                inlineInstr.preColoredTo = instr.preColoredTo;
            }
        }
        if (addLabel) {
            call.inlineCache.add(new LABEL("ret" + postFix));
        }
    }

    public String getInlineVarName(String varName) {
        String newName = replace.get(varName);
        if (Objects.equals(varName, "zero")) {
            return varName;
        }
        return Objects.requireNonNullElseGet(newName, () -> varName + postFix);
    }

    public void changeParaAndRet(Init init) {
        callPara.clear();
        for (int i = 0; i < init.paraList.size(); ++i) {
            var instr = init.paraList.get(i);
            instr.ignore = true;
            if (instr instanceof MV) {
                callPara.add(((MV) instr).to);
            } else if (instr instanceof LW) {
                callPara.add(((LW) instr).to);
            }
        }
        if (init.retInstr instanceof LI) {
            ((LI) init.retInstr).to = "ret$";
        } else if (init.retInstr instanceof MV) {
            ((MV) init.retInstr).to = "ret$";
        }
    }
}