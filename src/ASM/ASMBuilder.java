package src.ASM;

import src.ASM.instruction.*;
import src.ASM.instruction.binary.*;
import src.ASM.instruction.binaryImme.*;
import src.IR.IRProgram;
import src.IR.instruction.*;
import src.IR.statement.ConstString;
import src.IR.statement.FuncDef;
import src.IR.statement.GlobalVarDef;
import src.Util.cell.Cell;

import java.util.*;

import static java.lang.Math.min;

public class ASMBuilder {
    public static class FuncNode {//函数调用图
        public String funcName;
        public HashSet<FuncNode> fromNode;//被调用的函数
        public HashSet<FuncNode> toNode;//调用的函数
        public boolean restore = true;//true表示函数调用会改变t0-t7
        public List<CALL> callList;//被调用列表

        public FuncNode(String funcName_) {
            funcName = funcName_;
            fromNode = new HashSet<>();
            toNode = new HashSet<>();
            callList = new ArrayList<>();
        }
    }

    public static class PhiInfo {//用于处理关于phi的变量

        public static class PhiBlock {
            public Cell fromVar;
            public String toVar;

            public PhiBlock(Cell fromVar_, String toVar_) {
                fromVar = new Cell().set(fromVar_);
                toVar = toVar_;
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

        public void push(Cell fromVar_, String toVar_, String label_) {
            if (Objects.equals(label_, br.trueLabel.substring(1))) {
                phiTrueList.add(new PhiBlock(fromVar_, toVar_));
            } else {
                phiFalseList.add(new PhiBlock(fromVar_, toVar_));
            }
        }
    }

    public ASMProgram asmProgram;
    public HashSet<String> globalVar;
    public HashMap<String, FuncNode> funcNodeMap;
    public HashMap<String, PhiInfo> phiMap;//phi指令，跳转来源标签->目标标签及赋值语段，便于汇编处理
    public Queue<String> inlineQueue;
    public HashMap<String, String> globalVarReg;//存储全局变量的虚拟寄存器，仅每个块内各自有效
    public HashSet<String> globalVarStore;//需要store的全局变量，仅每个块内各自有效
    public int cnt = 0, label = 0;
    public Instruction cache;
    public HashMap<String, Getelementptr> getelememtptrMap;
    public HashMap<String, HashSet<String>> useGlobalVar;

    public ASMBuilder(IRProgram irProgram, HashMap<String, HashSet<String>> useGlobalVar_) {
        asmProgram = new ASMProgram();
        globalVar = new HashSet<>();
        funcNodeMap = new HashMap<>();
        inlineQueue = new ArrayDeque<>();
        globalVarReg = new HashMap<>();
        getelememtptrMap = new HashMap<>();
        globalVarStore = new HashSet<>();
        phiMap = new HashMap<>();
        useGlobalVar = useGlobalVar_;
        for (var stmt : irProgram.stmtList) {
            getelememtptrMap.clear();
            if (stmt instanceof FuncDef) {
                FuncNode funcNode = getNode(((FuncDef) stmt).functionName.substring(1));
                funcNode.restore = false;
                collectPhi((FuncDef) stmt);
                asmProgram.sectionText.pushGlobal(((FuncDef) stmt).functionName.substring(1));
                asmProgram.sectionText.nowFuncName = ((FuncDef) stmt).functionName.substring(1);
                asmProgram.sectionText.pushInstr(new LABEL(((FuncDef) stmt).functionName.substring(1), true));
                Init init = new Init();
                asmProgram.sectionText.pushInstr(init);
                //处理参数
                int size = ((FuncDef) stmt).parameterTypeList.size();
                if (size > 0) {
                    MV mv;
                    if (((FuncDef) stmt).isClassMethod) {
                        mv = new MV("tmp" + cnt++, "%this");
                    } else {
                        mv = new MV("tmp" + cnt++, "%_0");
                    }
                    mv.preColoredFrom = "a0";
                    asmProgram.sectionText.pushInstr(mv);
                    init.paraList.add(mv);
                }
                for (int i = 1; i < min(size, 8); ++i) {
                    MV mv;
                    if (((FuncDef) stmt).isClassMethod) {
                        mv = new MV("tmp" + cnt++, "%_" + (i - 1));
                    } else {
                        mv = new MV("tmp" + cnt++, "%_" + i);
                    }
                    mv.preColoredFrom = "a" + i;
                    asmProgram.sectionText.pushInstr(mv);
                    init.paraList.add(mv);
                }
                if (size > 8) {//栈上传递变量
                    for (int i = 8; i < size; ++i) {
                        LW lw;
                        if (((FuncDef) stmt).isClassMethod) {
                            lw = new LW("tmp" + cnt++, "%_" + (i - 1), (i - 8) << 2);
                        } else {
                            lw = new LW("tmp" + cnt++, "%_" + i, (i - 8) << 2);
                        }
                        lw.preColoredFrom = "stackTop#";
                        asmProgram.sectionText.pushInstr(lw);
                        init.paraList.add(lw);
                    }
                }
                for (var block : ((FuncDef) stmt).irList) {
                    if (!Objects.equals(block.label, "entry")) {
                        asmProgram.sectionText.pushInstr(new LABEL(block.label));
                        globalVarReg.clear();
                        globalVarStore.clear();
                    }
                    for (var instr : block.instrList) {
                        visitInstr(asmProgram.sectionText, instr, funcNode, init);
                    }
                }
            } else if (stmt instanceof GlobalVarDef) {
                asmProgram.sectionData.pushGlobal(((GlobalVarDef) stmt).varName.substring(1));
                asmProgram.sectionData.pushWord(((GlobalVarDef) stmt).varName.substring(1), (int) ((GlobalVarDef) stmt).value);
                globalVar.add(((GlobalVarDef) stmt).varName.substring(1));
            } else if (stmt instanceof ConstString) {
                for (var entry : ((ConstString) stmt).constStringMap.entrySet()) {
                    asmProgram.sectionRodata.pushConstString("constString-" + entry.getValue(), entry.getKey());
                    globalVar.add("constString-" + entry.getValue());
                }
            }
        }
        setFuncRestore();
    }

    void visitInstr(Section section, Instruction instruction, FuncNode funcNode, Init init) {
        if (cache instanceof Getelementptr) {
            if (instruction instanceof Store) {
                if (Objects.equals(((Store) instruction).toPointer, ((Getelementptr) cache).result)) {//直接通过下标偏移sw
                    ((Store) instruction).toPointer = ((Getelementptr) cache).from;
                    visit(section, (Store) instruction);
                    var tmpList = section.asmInstrList.get(section.asmInstrList.size() - 1);
                    var tmpInstr = tmpList.get(tmpList.size() - 1);
                    ((SW) tmpInstr).offset = (int) ((Getelementptr) cache).index.varValue << 2;
                } else {
                    visit(section, (Getelementptr) cache);
                    visit(section, (Store) instruction);
                }
                cache = null;
                return;
            } else if (instruction instanceof Load) {
                if (Objects.equals(((Load) instruction).fromPointer, ((Getelementptr) cache).result)) {//直接通过下标偏移lw
                    ((Load) instruction).fromPointer = ((Getelementptr) cache).from;
                    visit(section, (Load) instruction);
                    var tmpList = section.asmInstrList.get(section.asmInstrList.size() - 1);
                    var tmpInstr = tmpList.get(tmpList.size() - 1);
                    ((LW) tmpInstr).offset = (int) ((Getelementptr) cache).index.varValue << 2;
                } else {
                    visit(section, (Getelementptr) cache);
                    visit(section, (Load) instruction);
                }
                cache = null;
                return;
            } else {
                visit(section, (Getelementptr) cache);
                cache = null;
            }
        }
        if (instruction instanceof Store) {
            var getelementptr = getelememtptrMap.get(((Store) instruction).toPointer);
            if (getelementptr != null) {
                ((Store) instruction).toPointer = getelementptr.from;
                visit(section, (Store) instruction);
                var tmpList = section.asmInstrList.get(section.asmInstrList.size() - 1);
                var tmpInstr = tmpList.get(tmpList.size() - 1);
                ((SW) tmpInstr).offset = (int) getelementptr.index.varValue << 2;
            } else {
                visit(section, (Store) instruction);
            }
        } else if (instruction instanceof Load) {
            var getelementptr = getelememtptrMap.get(((Load) instruction).fromPointer);
            if (getelementptr != null) {
                ((Load) instruction).fromPointer = getelementptr.from;
                visit(section, (Load) instruction);
                var tmpList = section.asmInstrList.get(section.asmInstrList.size() - 1);
                var tmpInstr = tmpList.get(tmpList.size() - 1);
                ((LW) tmpInstr).offset = (int) getelementptr.index.varValue << 2;
            } else {
                visit(section, (Load) instruction);
            }
        } else if (instruction instanceof Binary) {
            visit(section, (Binary) instruction);
        } else if (instruction instanceof Icmp) {
            visit(section, (Icmp) instruction);
        } else if (instruction instanceof Call) {
            visit(section, (Call) instruction, funcNode);
        } else if (instruction instanceof Br) {
            visit(section, (Br) instruction);
        } else if (instruction instanceof Getelementptr) {
            if (((Getelementptr) instruction).index.isConst && ((Getelementptr) instruction).index.varValue < 512) {
                cache = instruction;
                getelememtptrMap.put(((Getelementptr) cache).result, (Getelementptr) cache);
            } else {
                visit(section, (Getelementptr) instruction);
            }
        } else if (instruction instanceof Ret) {
            visit(section, (Ret) instruction, init);
        }
    }

    void visit(Section section, Store store) {
        String from, to;
        boolean isStore = true;
        if (store.value.isConst) {
            if (store.value.varValue == 0) {
                from = "zero";
            } else {
                from = "tmp" + cnt++;
                section.pushInstr(new LI(from, (int) store.value.varValue));
            }
        } else {
            if (store.value.varName.charAt(0) == '@') {
                from = globalVarReg.get(store.value.varName.substring(1));
                if (from == null) {
                    from = "tmp" + cnt++;
                    if (store.value.varName.contains("-")) {
                        section.pushInstr(new LA(from, store.value.varName.substring(1)));
                    } else {
                        section.pushInstr(new LW(store.value.varName.substring(1), from));
                    }
                }
            } else {
                from = store.value.varName;
            }
        }
        if (store.toPointer.charAt(0) == '@') {
            globalVarStore.add(store.toPointer.substring(1));
            to = globalVarReg.get(store.toPointer.substring(1));
            if (to == null) {
                to = "tmp" + cnt++;
                section.pushInstr(new LA(to, store.toPointer.substring(1)));
            } else {
                isStore = false;
            }
        } else {
            to = store.toPointer;
        }
        if (isStore) {
            section.pushInstr(new SW(from, to, 0));
        } else {
            section.pushInstr(new MV(from, to));
        }
    }

    void visit(Section section, Load load) {
        if (load.fromPointer.charAt(0) == '@') {
            String varReg = globalVarReg.get(load.fromPointer.substring(1));
            if (varReg != null) {
                section.pushInstr(new MV(varReg, load.toVarName));
            } else {
                varReg = load.fromPointer.substring(1);
                if (load.fromPointer.contains("-")) {
                    section.pushInstr(new LA(load.toVarName, varReg));
                } else {
                    globalVarReg.put(varReg, load.toVarName);
                    section.pushInstr(new LW(varReg, load.toVarName));
                }
            }
        } else {
            section.pushInstr(new LW(load.fromPointer, load.toVarName, 0));
        }
    }

    void visit(Section section, Binary binary) {
        switch (binary.op) {
            case "add" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new ADD(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        if (binary.left.varValue == 0) {
                            section.pushInstr(new MV(binary.right.varName, binary.output));
                        } else {
                            section.pushInstr(new ADDI(binary.output, binary.right.varName, (int) binary.left.varValue));
                        }
                    } else if (!binary.left.isConst) {
                        if (binary.right.varValue == 0) {
                            section.pushInstr(new MV(binary.left.varName, binary.output));
                        } else {
                            section.pushInstr(new ADDI(binary.output, binary.left.varName, (int) binary.right.varValue));
                        }
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new ADDI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
            case "sub" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new SUB(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        if (binary.left.varValue == 0) {
                            section.pushInstr(new SUB("zero", binary.right.varName, binary.output));
                        } else {
                            section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                            section.pushInstr(new SUB("tmp" + cnt++, binary.right.varName, binary.output));
                        }
                    } else if (!binary.left.isConst) {
                        if (binary.right.varValue == 0) {
                            section.pushInstr(new MV(binary.left.varName, binary.output));
                        } else {
                            section.pushInstr(new ADDI(binary.output, binary.left.varName, -(int) binary.right.varValue));
                        }
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new ADDI(binary.output, "tmp" + cnt++, -(int) binary.right.varValue));
                    }
                }
            }
            case "mul" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new MUL(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        if (binary.left.varValue == 0) {
                            section.pushInstr(new MV("zero", binary.output));
                        } else if (binary.left.varValue == 1) {
                            section.pushInstr(new MV(binary.right.varName, binary.output));
                        } else if ((binary.left.varValue & (binary.left.varValue - 1)) == 0) {
                            int shift = -1, tmp = (int) binary.left.varValue;
                            while (tmp != 0) {
                                ++shift;
                                tmp = tmp >> 1;
                            }
                            section.pushInstr(new SLLI(binary.output, binary.right.varName, shift));
                        } else {
                            section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                            section.pushInstr(new MUL("tmp" + cnt++, binary.right.varName, binary.output));
                        }
                    } else if (!binary.left.isConst) {
                        if (binary.right.varValue == 0) {
                            section.pushInstr(new MV("zero", binary.output));
                        } else if (binary.right.varValue == 1) {
                            section.pushInstr(new MV(binary.left.varName, binary.output));
                        } else if ((binary.right.varValue & (binary.right.varValue - 1)) == 0) {
                            int shift = -1, tmp = (int) binary.right.varValue;
                            while (tmp != 0) {
                                ++shift;
                                tmp = tmp >> 1;
                            }
                            section.pushInstr(new SLLI(binary.output, binary.left.varName, shift));
                        } else {
                            section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                            section.pushInstr(new MUL(binary.left.varName, "tmp" + cnt++, binary.output));
                        }
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) binary.left.varValue));
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                        section.pushInstr(new MUL("tmp" + (cnt - 1), "tmp" + cnt++, binary.output));
                    }
                }
            }
            case "sdiv" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new DIV(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new DIV("tmp" + cnt++, binary.right.varName, binary.output));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                        section.pushInstr(new DIV(binary.left.varName, "tmp" + cnt++, binary.output));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) binary.left.varValue));
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                        section.pushInstr(new DIV("tmp" + (cnt - 1), "tmp" + cnt++, binary.output));
                    }
                }
            }
            case "srem" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new REM(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new REM("tmp" + cnt++, binary.right.varName, binary.output));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                        section.pushInstr(new REM(binary.left.varName, "tmp" + cnt++, binary.output));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) binary.left.varValue));
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.right.varValue));
                        section.pushInstr(new REM("tmp" + (cnt - 1), "tmp" + cnt++, binary.output));
                    }
                }
            }
            case "shl" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new SLL(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new SLL("tmp" + cnt++, binary.right.varName, binary.output));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new SLLI(binary.output, binary.left.varName, (int) binary.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new SLLI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
            case "ashr" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new SRA(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new SRA("tmp" + cnt++, binary.right.varName, binary.output));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new SRAI(binary.output, binary.left.varName, (int) binary.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new SRAI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
            case "and" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new AND(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new ANDI(binary.output, binary.right.varName, (int) binary.left.varValue));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new ANDI(binary.output, binary.left.varName, (int) binary.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new ANDI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
            case "or" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new OR(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new ORI(binary.output, binary.right.varName, (int) binary.left.varValue));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new ORI(binary.output, binary.left.varName, (int) binary.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new ORI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
            case "xor" -> {
                if (!binary.left.isConst && !binary.right.isConst) {
                    section.pushInstr(new XOR(binary.left.varName, binary.right.varName, binary.output));
                } else {
                    if (!binary.right.isConst) {
                        section.pushInstr(new XORI(binary.output, binary.right.varName, (int) binary.left.varValue));
                    } else if (!binary.left.isConst) {
                        section.pushInstr(new XORI(binary.output, binary.left.varName, (int) binary.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt, (int) binary.left.varValue));
                        section.pushInstr(new XORI(binary.output, "tmp" + cnt++, (int) binary.right.varValue));
                    }
                }
            }
        }
    }

    void visit(Section section, Icmp icmp) {
        SLT slt = null;
        SLTI slti = null;
        switch (icmp.cond) {
            case "slt", "sge" -> {
                if (!icmp.left.isConst && !icmp.right.isConst) {
                    slt = new SLT(icmp.left.varName, icmp.right.varName, "tmp" + cnt);
                    section.pushInstr(slt);
                } else {
                    if (!icmp.right.isConst) {
                        section.pushInstr(new LI("tmp" + cnt++, (int) icmp.left.varValue));
                        slt = new SLT("tmp" + (cnt - 1), icmp.right.varName, "tmp" + cnt);
                        section.pushInstr(slt);
                    } else if (!icmp.left.isConst) {
                        slti = new SLTI("tmp" + cnt, icmp.left.varName, (int) icmp.right.varValue);
                        section.pushInstr(slti);
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) icmp.left.varValue));
                        slti = new SLTI("tmp" + cnt, "tmp" + (cnt - 1), (int) icmp.right.varValue);
                        section.pushInstr(slti);
                    }
                }
                if (Objects.equals(icmp.cond, "sge")) {
                    section.pushInstr(new XORI(icmp.output, "tmp" + cnt++, 1));
                } else {
                    if (slt != null) {
                        slt.to = icmp.output;
                    } else {
                        slti.to = icmp.output;
                    }
                }
            }
            case "sgt", "sle" -> {
                if (!icmp.left.isConst && !icmp.right.isConst) {
                    slt = new SLT(icmp.right.varName, icmp.left.varName, "tmp" + cnt);
                    section.pushInstr(slt);
                } else {
                    if (!icmp.right.isConst) {
                        slti = new SLTI("tmp" + cnt, icmp.right.varName, (int) icmp.left.varValue);
                        section.pushInstr(slti);
                    } else if (!icmp.left.isConst) {
                        section.pushInstr(new LI("tmp" + cnt++, (int) icmp.right.varValue));
                        slt = new SLT("tmp" + (cnt - 1), icmp.left.varName, "tmp" + cnt);
                        section.pushInstr(slt);
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) icmp.right.varValue));
                        slti = new SLTI("tmp" + cnt, "tmp" + (cnt - 1), (int) icmp.left.varValue);
                        section.pushInstr(slti);
                    }
                }
                if (Objects.equals(icmp.cond, "sle")) {
                    section.pushInstr(new XORI(icmp.output, "tmp" + cnt++, 1));
                } else {
                    if (slt != null) {
                        slt.to = icmp.output;
                    } else {
                        slti.to = icmp.output;
                    }
                }
            }
            case "eq", "ne" -> {
                if (!icmp.left.isConst && !icmp.right.isConst) {
                    section.pushInstr(new XOR(icmp.left.varName, icmp.right.varName, "tmp" + cnt));
                } else {
                    if (!icmp.right.isConst) {
                        section.pushInstr(new XORI("tmp" + cnt, icmp.right.varName, (int) icmp.left.varValue));
                    } else if (!icmp.left.isConst) {
                        section.pushInstr(new XORI("tmp" + cnt, icmp.left.varName, (int) icmp.right.varValue));
                    } else {
                        section.pushInstr(new LI("tmp" + cnt++, (int) icmp.left.varValue));
                        section.pushInstr(new XORI("tmp" + cnt, "tmp" + (cnt - 1), (int) icmp.right.varValue));
                    }
                }
                if (Objects.equals(icmp.cond, "eq")) {
                    section.pushInstr(new SEQZ("tmp" + cnt++, icmp.output));
                } else {
                    section.pushInstr(new SNEZ("tmp" + cnt++, icmp.output));
                }
            }
        }
    }

    void visit(Section section, Call call, FuncNode funcNode) {
        for (var key : useGlobalVar.keySet()) {
            String valueVar = globalVarReg.remove(key);
            if (valueVar != null && globalVarStore.contains(key)) {
                globalVarStore.remove(key);
                section.pushInstr(new LA("tmp" + cnt, key));
                section.pushInstr(new SW(valueVar, "tmp" + cnt++, 0));
            }
        }
        FuncNode callNode = getNode(call.functionName.substring(1));
        callNode.fromNode.add(funcNode);
        funcNode.toNode.add(callNode);
        int size = call.callTypeList.size();
        Cell variable;
        CallerSave callerSave = new CallerSave(size);
        CALL ASMcall = new CALL(call.functionName.substring(1));
        for (int i = 0; i < min(size, 8); ++i) {
            variable = call.callList.get(i);
            if (variable.varName != null) {
                String from;
                if (variable.varName.charAt(0) == '@') {
                    from = globalVarReg.get(variable.varName.substring(1));
                    if (from == null) {
                        from = "tmp" + cnt++;
                        if (variable.varName.contains("-")) {
                            section.pushInstr(new LA(from, variable.varName.substring(1)));
                        } else {
                            globalVarReg.put(variable.varName.substring(1), from);
                            section.pushInstr(new LW(variable.varName.substring(1), from));
                        }
                    }
                } else {
                    from = variable.varName;
                }
                MV mv = new MV(from, "tmp" + cnt);
                mv.preColoredTo = "a" + i;
                section.pushInstr(mv);
                ASMcall.paraList.add(mv);
            } else {
                LI li = new LI("tmp" + cnt, (int) variable.varValue);
                li.preColoredTo = "a" + i;
                section.pushInstr(li);
                ASMcall.paraList.add(li);
            }
            ASMcall.useList.add("tmp" + cnt++);
        }
        if (size > 8) {
            for (int i = 8; i < size; ++i) {
                variable = call.callList.get(i);
                if (variable.varName != null) {
                    String from;
                    if (variable.varName.charAt(0) == '@') {
                        from = globalVarReg.get(variable.varName.substring(1));
                        if (from == null) {
                            from = "tmp" + cnt++;
                            if (variable.varName.contains("-")) {
                                section.pushInstr(new LA(from, variable.varName.substring(1)));
                            } else {
                                globalVarReg.put(variable.varName.substring(1), from);
                                section.pushInstr(new LW(variable.varName.substring(1), from));
                            }
                        }
                    } else {
                        from = variable.varName;
                    }
                    SW sw = new SW(from, "tmp" + cnt++, (i - 8) << 2);
                    section.pushInstr(sw);
                    sw.preColoredTo = "sp";
                    ASMcall.paraList.add(sw);
                } else {
                    section.pushInstr(new LI("tmp" + cnt++, (int) variable.varValue));
                    SW sw = new SW("tmp" + (cnt - 1), "tmp" + cnt++, (i - 8) << 2);
                    section.pushInstr(sw);
                    sw.preColoredTo = "sp";
                    ASMcall.paraList.add(sw);
                }
            }
        }
        section.pushInstr(callerSave);
        section.pushInstr(ASMcall);
        callNode.callList.add(ASMcall);
        if (call.resultVar != null) {
            MV mv = new MV("tmp" + cnt, call.resultVar);
            ASMcall.def = "tmp" + cnt++;
            mv.preColoredFrom = "a0";
            section.pushInstr(mv);
            ASMcall.retMV = mv;
        }
        section.pushInstr(new CallerRestore(callerSave, call.functionName.substring(1)));
    }

    void visit(Section section, Br br) {
        for (var entry : globalVarReg.entrySet()) {
            if (globalVarStore.contains(entry.getKey())) {
                section.pushInstr(new LA("tmp" + cnt, entry.getKey()));
                section.pushInstr(new SW(entry.getValue(), "tmp" + cnt++, 0));
            }
        }
        globalVarReg.clear();
        globalVarStore.clear();
        var phiInfo = phiMap.get(br.nowLabel.substring(1));
        if (br.condition == null) {
            if (phiInfo != null) {
                visit(section, phiInfo.phiTrueList);
            }
            section.pushInstr(new J(br.trueLabel.substring(1)));
        } else {
            boolean flag = phiInfo != null && phiInfo.phiTrueList.size() > 0;
            if (flag) {
                section.pushInstr(new BNEZ(br.condition, "tmpLabel" + label));
            } else {
                section.pushInstr(new BNEZ(br.condition, br.trueLabel.substring(1)));
            }
            if (phiInfo != null && phiInfo.phiFalseList.size() > 0) {
                visit(section, phiInfo.phiFalseList);
            }
            section.pushInstr(new J(br.falseLabel.substring(1)));
            if (flag) {
                section.pushInstr(new LABEL("tmpLabel" + label++));
                visit(section, phiInfo.phiTrueList);
                section.pushInstr(new J(br.trueLabel.substring(1)));
            }
        }
    }

    void visit(Section section, List<PhiInfo.PhiBlock> phiList) {
        List<String> tmpVarList = new ArrayList<>();//中间变量
        List<String> toVarList = new ArrayList<>();//目标变量
        for (var phi : phiList) {
            if (phi.fromVar.isConst) {
                LI li = new LI(phi.toVar, (int) phi.fromVar.varValue);
                if (phi.toVar.contains(".returnValue")) {
                    li.notRemove = true;
                }
                section.pushInstr(li);
            } else {
                String from;
                if (phi.fromVar.varName.contains("@")) {
                    from = globalVarReg.get(phi.fromVar.varName.substring(1));
                    if (from == null) {
                        from = "tmp" + cnt++;
                        if (phi.fromVar.varName.contains("-")) {
                            section.pushInstr(new LA(from, phi.fromVar.varName.substring(1)));
                        } else {
                            section.pushInstr(new LW(phi.fromVar.varName.substring(1), from));
                        }
                    }
                } else {
                    from = phi.fromVar.varName;
                }
                MV mv = new MV(from, "tmp" + cnt);
                if (phi.toVar.contains(".returnValue")) {
                    mv.notRemove = true;
                }
                section.pushInstr(mv);
                tmpVarList.add("tmp" + cnt++);
                toVarList.add(phi.toVar);
            }
        }
        for (int i = 0; i < tmpVarList.size(); ++i) {
            section.pushInstr(new MV(tmpVarList.get(i), toVarList.get(i)));
        }
    }

    void visit(Section section, Getelementptr getelementptr) {
        if (getelementptr.index.isConst) {
            section.pushInstr(new ADDI(getelementptr.result, getelementptr.from, (int) getelementptr.index.varValue << 2));
        } else {
            section.pushInstr(new SLLI("tmp" + cnt, getelementptr.index.varName, 2));
            section.pushInstr(new ADD(getelementptr.from, "tmp" + cnt++, getelementptr.result));
        }
    }

    void visit(Section section, Ret ret, Init init) {
        for (var entry : globalVarReg.entrySet()) {
            if (globalVarStore.contains(entry.getKey())) {
                section.pushInstr(new LA("tmp" + cnt, entry.getKey()));
                section.pushInstr(new SW(entry.getValue(), "tmp" + cnt++, 0));
            }
        }
        globalVarReg.clear();
        globalVarStore.clear();
        if (ret.irType != null && ret.irType.unitSize != -1) {
            if (ret.retVar != null && !ret.retVar.isConst) {
                MV mv = new MV(ret.retVar.varName, "tmp" + cnt++);
                mv.notRemove = true;
                mv.preColoredTo = "a0";
                section.pushInstr(mv);
                init.retInstr = mv;
            } else {
                assert ret.retVar != null;
                LI li = new LI("tmp" + cnt++, (int) ret.retVar.varValue);
                li.notRemove = true;
                li.preColoredTo = "a0";
                section.pushInstr(li);
                init.retInstr = li;
            }
        }
        asmProgram.sectionText.pushInstr(new Restore());
        asmProgram.sectionText.pushInstr(new RET());
    }

    public FuncNode getNode(String funcName) {
        var node = funcNodeMap.get(funcName);
        if (node == null) {
            node = new FuncNode(funcName);
            funcNodeMap.put(funcName, node);
        }
        return node;
    }

    void setFuncRestore() {
        HashSet<FuncNode> visit = new HashSet<>();
        Queue<FuncNode> queue = new ArrayDeque<>();
        for (var func : funcNodeMap.values()) {//将未调用除内建函数外其他函数的函数入队
            if (func.toNode.size() == 0) {
                if (!func.restore && !Objects.equals(func.funcName, "main")) {
                    inlineQueue.add(func.funcName);
                }
            } else {
                boolean flag = true;
                for (var toNode : func.toNode) {
                    if (!toNode.restore) {
                        flag = false;
                        break;
                    }
                }
                if (flag && !Objects.equals(func.funcName, "main")) {
                    inlineQueue.add(func.funcName);
                }
            }
        }
        for (var func : funcNodeMap.values()) {
            if (!visit.contains(func)) {
                if (func.restore) {
                    queue.add(func);
                    visit.add(func);
                    while (!queue.isEmpty()) {
                        FuncNode node = queue.poll();
                        if (node.restore) {
                            node.fromNode.forEach(funcNode -> {
                                if (!visit.contains(funcNode)) {
                                    funcNode.restore = true;
                                    queue.add(funcNode);
                                    visit.add(funcNode);
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private void collectPhi(FuncDef funcDef) {
        phiMap.clear();
        Stack<Block> blockStack = new Stack<>();
        Stack<Phi> phiStack = new Stack<>();
        for (var block : funcDef.irList) {
            for (var instr : block.instrList) {
                if (instr instanceof Br) {
                    phiMap.put(((Br) instr).nowLabel.substring(1), new PhiInfo((Br) instr));
                } else if (instr instanceof Phi) {
                    blockStack.push(block);
                    phiStack.push((Phi) instr);
                }
            }
        }
        while (!phiStack.isEmpty()) {
            var phi = phiStack.pop();
            var block = blockStack.pop();
            for (var assignBlock : phi.assignBlockList) {
                var phiInfo = phiMap.get(assignBlock.label.substring(1));
                if (phiInfo != null) {
                    phiInfo.push(assignBlock.variable, phi.result, block.label);
                }
            }
        }
    }

}
