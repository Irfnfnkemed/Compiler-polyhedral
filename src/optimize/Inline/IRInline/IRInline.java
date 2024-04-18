package src.optimize.Inline.IRInline;

import src.IR.IRProgram;
import src.IR.instruction.*;
import src.IR.statement.FuncDef;
import src.IR.statement.IRStatement;
import src.Util.cell.Cell;
import src.Util.type.IRType;
import src.optimize.ADCE.FunctionADCE;
import src.optimize.Mem2Reg.CFGDom;
import src.optimize.Mem2Reg.Dom;
import src.optimize.Mem2Reg.PutPhi;
import src.optimize.SCCP.FunctionSCCP;
import src.optimize.SCCP.SCCP;

import java.util.*;

import static java.lang.Math.min;
import static src.optimize.Mem2Reg.Mem2Reg.merge;

public class IRInline {

    public class FuncNode {
        public FuncDef funcDef;
        public HashMap<String, List<Call>> callList;// 调用函数名->对应call指令
        public Ret retInstr;
        public int dfn = -1, low = -2;
        public boolean visited = false;
        public int superNodeId = -1;
        public int size = -1;
        public boolean inline = false;
        public boolean rebuild = false;

        public FuncNode(FuncDef funcDef_) {
            funcDef = funcDef_;
            callList = new HashMap<>();
            for (var block : funcDef.irList) {
                for (var instr : block.instrList) {
                    if (instr instanceof Call) {
                        if (!builtinFunc.contains(((Call) instr).functionName)) {
                            callList.computeIfAbsent(((Call) instr).functionName,
                                    k -> new ArrayList<>()).add((Call) instr);
                            instr.extraBase = new ExtraInline(block);
                        }
                    } else if (instr instanceof Ret) {
                        retInstr = (Ret) instr;
                    }
                }
            }
        }
    }

    public static class SuperNode {
        public List<String> funcNodes;
        public int outDegree = 0;
        public HashSet<Integer> preSuperNode;

        public SuperNode() {
            funcNodes = new ArrayList<>();
            preSuperNode = new HashSet<>();
        }
    }

    public final IRProgram irProgram;
    public final HashSet<String> builtinFunc = new HashSet<>() {{
        add("@print");
        add("@println");
        add("@printInt");
        add("@printlnInt");
        add("@getString");
        add("@getInt");
        add("@toString");
        add("@string.length");
        add("@string.substring");
        add("@string.parseInt");
        add("@string.ord");
        add("@string.add");
        add("@string.equal");
        add("@string.notEqual");
        add("@string.less");
        add("@string.lessOrEqual");
        add("@string.greater");
        add("@greaterOrEqual");
        add("@array.size");
        add("@.malloc");
    }};
    public HashMap<String, FuncNode> funcNodes;
    private int dfn = 1;
    private Stack<String> dfsStack;
    private HashSet<String> dfsStackSet;
    private List<SuperNode> superNodes;
    private HashMap<String, Cell> replace;
    private int cnt = 1;
    private String postfix;


    public IRInline(IRProgram irProgram_) {
        irProgram = irProgram_;
        funcNodes = new HashMap<>();
        dfsStack = new Stack<>();
        dfsStackSet = new HashSet<>();
        superNodes = new ArrayList<>();
        replace = new HashMap<>();
    }

    public void optimize() {
        setFuncCallGraph();
        inline();
    }

    private void setFuncCallGraph() {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                funcNodes.put(((FuncDef) stmt).functionName, new FuncNode((FuncDef) stmt));
            }
        }
        Tarjan("@main");
        for (var funcNode : funcNodes.values()) {
            if (funcNode.dfn != -1) {//main中会调用到
                for (String callFuncName : funcNode.callList.keySet()) {
                    var callFuncNode = funcNodes.get(callFuncName);
                    if (callFuncNode.superNodeId != funcNode.superNodeId &&
                            !superNodes.get(callFuncNode.superNodeId).preSuperNode.contains(funcNode.superNodeId)) {
                        ++superNodes.get(funcNode.superNodeId).outDegree;
                        superNodes.get(callFuncNode.superNodeId).preSuperNode.add(funcNode.superNodeId);
                    }
                }
            }
        }
    }

    private void Tarjan(String funcName) {
        FuncNode funcNode = funcNodes.get(funcName);
        funcNode.visited = true;
        funcNode.dfn = dfn++;
        funcNode.low = funcNode.dfn;
        dfsStack.push(funcName);
        dfsStackSet.add(funcName);
        for (String callFuncName : funcNode.callList.keySet()) {
            FuncNode callFunNode = funcNodes.get(callFuncName);
            if (!callFunNode.visited) {
                Tarjan(callFuncName);
                funcNode.low = min(funcNode.low, callFunNode.low);
            } else if (dfsStackSet.contains(callFuncName)) {
                funcNode.low = min(funcNode.low, callFunNode.dfn);
            }
        }
        if (funcNode.low == funcNode.dfn) {
            SuperNode superNode = new SuperNode();
            superNodes.add(superNode);
            String tmp = dfsStack.pop();
            dfsStackSet.remove(tmp);
            superNode.funcNodes.add(tmp);
            funcNodes.get(tmp).superNodeId = superNodes.size() - 1;
            while (!tmp.equals(funcName)) {
                tmp = dfsStack.pop();
                dfsStackSet.remove(tmp);
                superNode.funcNodes.add(tmp);
                funcNodes.get(tmp).superNodeId = superNodes.size() - 1;
            }
        }
    }

    private void inline() {
        Queue<SuperNode> workQueue = new ArrayDeque<>();
        for (var superNode : superNodes) {
            if (superNode.outDegree == 0) {
                workQueue.add(superNode);
            }
        }
        while (!workQueue.isEmpty()) {
            var superNode = workQueue.poll();
            if (superNode.funcNodes.size() == 1 &&
                    !funcNodes.get(superNode.funcNodes.get(0)).callList.containsKey(superNode.funcNodes.get(0))) {
                FuncDef inlineFunc = funcNodes.get(superNode.funcNodes.get(0)).funcDef;
                rebuild(inlineFunc);
                if (funcNodes.get(inlineFunc.functionName).size < 250) {//superNode仅有一个函数节点，且无自环，长度合适，可以内联
                    funcNodes.get(inlineFunc.functionName).inline = true;
                    for (int id : superNode.preSuperNode) {
                        var tarSuperNode = superNodes.get(id);
                        for (String funcName : tarSuperNode.funcNodes) {
                            var callList = funcNodes.get(funcName).callList.get(inlineFunc.functionName);
                            if (callList != null) {
                                for (var call : callList) {
                                    inlineFunc(call, inlineFunc);
                                }
                            }
                        }
                    }
                }
            } else {
                for (String funcName : superNode.funcNodes) {
                    rebuild(funcNodes.get(funcName).funcDef);
                }
            }
            for (var id : superNode.preSuperNode) {
                int tmp = --superNodes.get(id).outDegree;
                if (tmp == 0) {
                    workQueue.add(superNodes.get(id));
                }
            }
        }
        List<IRStatement> newStmtList = new ArrayList<>();
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                var tmp = funcNodes.get(((FuncDef) stmt).functionName);
                if (((FuncDef) stmt).functionName.equals("@main") || (!tmp.inline && tmp.dfn != -1)) {
                    newStmtList.add(stmt);
                }
            } else {
                newStmtList.add(stmt);
            }
        }
        irProgram.stmtList = newStmtList;
    }

    private void inlineFunc(Call call, FuncDef funcDef) {
        replace.clear();
        postfix = "-inlineIR" + cnt++;
        ((ExtraInline) call.extraBase).block.extraBase = new ExtraBlock(true);
        var insertList = ((ExtraInline) call.extraBase).insertList;
        int size = funcDef.parameterTypeList.size();
        if (size > 0) {
            if (funcDef.isClassMethod) {
                replace.put("%this", call.callList.get(0));
            } else {
                replace.put("%_0", call.callList.get(0));
            }
        }
        for (int i = 1; i < size; ++i) {
            if (funcDef.isClassMethod) {
                replace.put("%_" + (i - 1), call.callList.get(i));
            } else {
                replace.put("%_" + i, call.callList.get(i));
            }
        }
        Block nowBlock, retBlock;
        for (var block : funcDef.irList) {
//            if (block.instrList.get(block.instrList.size() - 1) instanceof Ret) {
//                retBlock = block;
//                continue;
//            }
            insertList.add(nowBlock = new Block(block.label + postfix));
            for (var instr : block.instrList) {
                if (instr instanceof Phi) {
                    Phi phi = new Phi((Phi) instr);
                    for (var assign : phi.assignBlockList) {
                        replace(assign.variable);
                        assign.label += postfix;
                    }
                    phi.result = replace(phi.result);
                    nowBlock.instrList.add(phi);
                } else if (instr instanceof Binary) {
                    Binary binary = new Binary((Binary) instr);
                    replace(binary.left);
                    replace(binary.right);
                    binary.output = replace(binary.output);
                    nowBlock.instrList.add(binary);
                } else if (instr instanceof Icmp) {
                    Icmp icmp = new Icmp((Icmp) instr);
                    replace(icmp.left);
                    replace(icmp.right);
                    icmp.output = replace(icmp.output);
                    nowBlock.instrList.add(icmp);
                } else if (instr instanceof Load) {
                    Load load = new Load((Load) instr);
                    load.fromPointer = replace(load.fromPointer);
                    load.toVarName = replace(load.toVarName);
                    nowBlock.instrList.add(load);
                } else if (instr instanceof Store) {
                    Store store = new Store((Store) instr);
                    replace(store.value);
                    store.toPointer = replace(store.toPointer);
                    nowBlock.instrList.add(store);
                } else if (instr instanceof Getelementptr) {
                    Getelementptr getelementptr = new Getelementptr((Getelementptr) instr);
                    getelementptr.from = replace(getelementptr.from);
                    getelementptr.result = replace(getelementptr.result);
                    replace(getelementptr.index);
                    nowBlock.instrList.add(getelementptr);
                } else if (instr instanceof Call) {
                    Call call_ = new Call((Call) instr);
                    for (var cell : call_.callList) {
                        replace(cell);
                    }
                    call_.resultVar = replace(call_.resultVar);
                    nowBlock.instrList.add(call_);
                } else if (instr instanceof Br) {
                    Br br = new Br((Br) instr);
                    br.nowLabel += postfix;
                    br.trueLabel += postfix;
                    if (br.condition != null) {
                        br.falseLabel += postfix;
                    }
                    br.condition = replace(br.condition);
                    nowBlock.instrList.add(br);
                } else if (instr instanceof Ret) {
                    ((ExtraInline) call.extraBase).toLabel = "out-block" + postfix;
                    ((ExtraInline) call.extraBase).fromLabel = nowBlock.label;
                    if (((Ret) instr).retVar != null) {
                        ((ExtraInline) call.extraBase).toCell = new Cell().set(((Ret) instr).retVar);
                        replace(((ExtraInline) call.extraBase).toCell);
                    }
                    Br br = new Br(nowBlock.label, "out-block" + postfix);
                    nowBlock.instrList.add(br);
                }
            }
        }
    }

    private String replace(String varName) {
        if (varName == null) {
            return null;
        }
        Cell tmp = replace.get(varName);
        if (tmp == null) {
            if (!varName.contains("@")) {
                return varName + postfix;
            } else {
                return varName;
            }
        } else {
            return tmp.varName;
        }
    }

    private void replace(Cell cell) {
        if (!cell.isConst) {
            Cell tmp = replace.get(cell.varName);
            if (tmp == null) {
                if (!cell.varName.contains("@")) {
                    cell.set(cell.varName + postfix);
                }
            } else {
                cell.set(tmp);
            }
        }
    }

    private void rebuild(FuncDef funcDef) {
        var funcNode = funcNodes.get(funcDef.functionName);
        if (funcNode.rebuild) {
            return;
        } else {
            funcNode.rebuild = true;
        }
        HashMap<String, Block> blockMap = new HashMap<>();
        for (var block : funcDef.irList) {
            blockMap.put(block.label, block);
        }
        List<Block> newIrList = new ArrayList<>();
        Block retBlock = null;
        int size = funcDef.irList.size();
        int x = 1;
        for (int i = 0; i <= size; ++i) {
            Block block;
            if (i == size && retBlock != null) {
                block = retBlock;
            } else {
                block = funcDef.irList.get(i);
                if (block.instrList.get(block.instrList.size() - 1) instanceof Ret) {
                    retBlock = block;
                    continue;
                }
            }
            if (block.extraBase instanceof ExtraBlock && ((ExtraBlock) block.extraBase).inline) {
                Block nowBlock = new Block(block.label);
                newIrList.add(nowBlock);
                blockMap.put(nowBlock.label, nowBlock);//更新map
                for (var instr : block.instrList) {
                    if (instr instanceof Call) {
                        if (instr.extraBase instanceof ExtraInline) {
                            var insertList = ((ExtraInline) instr.extraBase).insertList;
                            if (insertList.size() > 0) {
                                nowBlock.instrList.add(new Br(nowBlock.label, insertList.get(0).label));
                                newIrList.addAll(insertList);
                                nowBlock = new Block(((ExtraInline) instr.extraBase).toLabel);
                                newIrList.add(nowBlock);
                                if (((ExtraInline) instr.extraBase).toCell != null) {
                                    var tmp = (ExtraInline) instr.extraBase;
                                    Phi phi = new Phi(new IRType(((Call) instr).irType), ((Call) instr).resultVar);
                                    phi.assignBlockList.add(new Phi.assignBlock(tmp.toCell, "%" + tmp.fromLabel));
                                    nowBlock.instrList.add(phi);
                                }
                            } else {
                                nowBlock.instrList.add(instr);
                            }
                        } else {
                            nowBlock.instrList.add(instr);
                        }
                    } else {
                        nowBlock.instrList.add(instr);
                        if (instr instanceof Br) {
                            Block toBlock = blockMap.get(((Br) instr).trueLabel.substring(1));
                            for (var instrTo : toBlock.instrList) {
                                if (instrTo instanceof Phi) {//更新可达的phi
                                    for (var assign : ((Phi) instrTo).assignBlockList) {
                                        if (assign.label.equals(((Br) instr).nowLabel)) {
                                            assign.label = "%" + nowBlock.label;
                                            break;
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            if (((Br) instr).condition != null) {
                                toBlock = blockMap.get(((Br) instr).falseLabel.substring(1));
                                for (var instrTo : toBlock.instrList) {
                                    if (instrTo instanceof Phi) {//更新可达的phi
                                        for (var assign : ((Phi) instrTo).assignBlockList) {
                                            if (assign.label.equals(((Br) instr).nowLabel)) {
                                                assign.label = "%" + nowBlock.label;
                                                break;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                            ((Br) instr).nowLabel = nowBlock.label;//更新原块结尾br
                        }
                    }
                }
            } else {
                newIrList.add(block);
            }
        }
        funcDef.irList = newIrList;
        //TODO:OPT THIS SECTION
        CFGDom cfg = new CFGDom(funcDef, false);
        Dom dom = new Dom(cfg);
        PutPhi putPhi = new PutPhi(dom, funcDef);
        merge(funcDef, cfg);
        FunctionSCCP functionSCCP = new FunctionSCCP(funcDef);
        functionSCCP.optimize();
//
        ////////
        int funcSize = 0;
        for (var block : funcDef.irList) {
            funcSize += block.instrList.size();
        }
        funcNodes.get(funcDef.functionName).size = funcSize;
    }
}
