package src.IR;

import src.AST.ASTVisitor;
import src.AST.Program;
import src.AST.definition.*;
import src.AST.definition.variableDef.InitVariable;
import src.AST.definition.variableDef.VariableDef;
import src.AST.expression.*;
import src.AST.statement.Statement;
import src.AST.statement.Suite;
import src.AST.statement.jumpStatement.BreakStmt;
import src.AST.statement.jumpStatement.ContinueStmt;
import src.AST.statement.jumpStatement.ReturnStmt;
import src.AST.statement.loopStatement.ForLoop;
import src.AST.statement.loopStatement.WhileLoop;
import src.AST.statement.selectStatement.SelectStatement;
import src.IR.instruction.*;
import src.IR.statement.ClassTypeDef;
import src.IR.statement.FuncDef;
import src.IR.statement.GlobalVarDef;
import src.Util.cell.Cell;
import src.Util.scope.GlobalScope;
import src.Util.type.IRType;
import src.Util.type.Type;
import src.polyhedral.dependency.Model;
import src.polyhedral.extract.Domain;

import java.util.*;

public class IRBuilder implements ASTVisitor {
    public IRProgram irProgram;
    public GlobalScope globalScope;
    public HashSet<String> inlineGlobalVar;//内联到main函数的全局变量
    public HashMap<String, HashSet<String>> useGlobalVar;//函数名->用到的全局变量
    public FuncDef funcMain;
    public IRNode now;
    public int anonymousVar = 0;
    public int anonymousLabel = 0;
    public int anonymousMain = 0;

    public IRType typeI32;
    public IRType typeI1;
    public IRType typePtr;

    public IRBuilder(Program node, GlobalScope globalScope_, HashSet<String> inlineGlobalVar_) {
        globalScope = globalScope_;
        inlineGlobalVar = inlineGlobalVar_;
        useGlobalVar = new HashMap<>();
        typeI32 = new IRType().setI32();
        typeI1 = new IRType().setI1();
        typePtr = new IRType().setPtr();
        irProgram = new IRProgram();
        funcMain = new FuncDef();
        funcMain.pushBlock(new Block("entry"));
        funcMain.irType = typeI32;
        funcMain.functionName = "@main";
        visit(node);
        recollect();
    }

    @Override
    public void visit(Program node) {
        FuncDef funcDef = new FuncDef();
        funcDef.pushBlock(new Block("entry"));
        funcDef.irType = typePtr;
        funcDef.functionName = "@.newArray";
        funcDef.parameterTypeList.add(typeI32);
        Binary binary = new Binary("+");
        binary.set(1);
        binary.set("%_0");
        binary.output = "%_1";
        funcDef.push(binary);
        Call call = new Call("@.malloc");
        call.set(typeI32, "%_1");
        call.irType = typePtr;
        call.resultVar = "%_2";
        funcDef.push(call);
        funcDef.push(new Store(typeI32, "%_0", "%_2"));
        funcDef.push(new Getelementptr("%_3", typePtr, "%_2", -1, 1));
        funcDef.push(new Ret(typePtr, "%_3"));
        irProgram.push(funcDef);
        now = irProgram;
        node.defList.forEach(def -> def.accept(this));
    }

    @Override
    public void visit(Definition node) {
        if (node == null) {
            return;
        }
        if (node.mainDef != null) {
            useGlobalVar.put("main", new HashSet<>());
            node.mainDef.accept(this);
        } else if (node.classDef != null) {
            node.classDef.accept(this);
        } else if (node.functionDef != null) {
            useGlobalVar.put(node.functionDef.functionName, new HashSet<>());
            node.functionDef.accept(this);
        } else if (node.variableDef != null) {
            node.variableDef.accept(this);
        }
    }

    @Override
    public void visit(MainDef node) {
        funcMain.push(new Alloca(typeI32, "%.returnValue"));
        funcMain.push(new Store(typeI32, 0, "%.returnValue"));
        anonymousVar = anonymousMain;
        anonymousLabel = 0;
        irProgram.push(funcMain);
        var nowTmp = now;
        now = funcMain;
        node.suite.accept(this);
        if (node.scope.notReturn) {
            funcMain.push(new Br("%returnLabel", funcMain));
        }
        funcMain.pushBlock(new Block("returnLabel"));
        funcMain.push(new Load(typeI32, anonymousVariable(anonymousVar), "%.returnValue"));
        funcMain.push(new Ret(typeI32, anonymousVariable(anonymousVar)));
        now = nowTmp;
    }

    @Override
    public void visit(ClassDef node) {
        ClassTypeDef classTypeDef = new ClassTypeDef(node.className, node.variableDefList);
        now = classTypeDef;
        irProgram.push(classTypeDef);
        if (node.constructor == null) {
            node.constructor = new Constructor();
            node.constructor.className = node.className;
        }
        node.constructor.accept(this);
        node.functionDefList.forEach(functionDef -> {
            functionDef.accept(this);
            var funcDef = (FuncDef) irProgram.stmtList.get(irProgram.stmtList.size() - 1);
            funcDef.parameterTypeList.add(0, typePtr);
            funcDef.isClassMethod = true;
        });
        now = irProgram;
    }

    @Override
    public void visit(Constructor node) {
        anonymousVar = 0;
        anonymousLabel = 0;
        var nowTmp = now;
        FuncDef funcDef = new FuncDef();
        funcDef.pushBlock(new Block("entry"));
        funcDef.isClassMethod = true;
        funcDef.irType = typePtr;
        funcDef.functionName = "@.init-class-" + node.className;
        useGlobalVar.put(funcDef.functionName.substring(1), new HashSet<>());
        Call call = new Call("@.malloc");
        call.irType = typePtr;
        call.set(typeI32, globalScope.getClassSize(node.className));
        call.resultVar = "%this";
        funcDef.push(call);
        ((ClassTypeDef) nowTmp).variableDefList.forEach(variableDef -> {
            variableDef.initVariablelist.forEach(initVariable -> {
                Exp exp = new Exp(funcDef);
                now = exp;
                if (initVariable.exp != null) {
                    initVariable.exp.accept(this);
                }
                funcDef.push(new Getelementptr(anonymousVariable(anonymousVar), new IRType().setClass(node.className),
                        "%this", 0, globalScope.getClassMemberId(node.className, initVariable.variableName)));
                if (initVariable.exp == null) {
                    funcDef.push(new Store(new IRType(initVariable.type), 0, anonymousVariable(anonymousVar++)));
                } else {
                    funcDef.push(new Store(new IRType(initVariable.type), exp.pop(), anonymousVariable(anonymousVar++)));
                }
            });
        });

        now = funcDef;
        if (node.suite != null) {
            node.suite.accept(this);
            if (node.scope.notReturn) {
                funcDef.push(new Br("%returnLabel", funcDef));
            }
        } else {
            funcDef.push(new Br("%returnLabel", funcDef));
        }
        funcDef.pushBlock(new Block("returnLabel"));
        funcDef.push(new Ret(typePtr, "%this"));
        now = nowTmp;
        irProgram.push(funcDef);
    }

    @Override
    public void visit(FunctionDef node) {
        anonymousVar = 0;
        anonymousLabel = 0;
        FuncDef funcDef = new FuncDef();
        if (node.scope.isClass) {
            funcDef.functionName = "@" + node.scope.classType.typeName + "." + node.functionName;
            useGlobalVar.put(funcDef.functionName.substring(1), new HashSet<>());
        } else {
            funcDef.functionName = "@" + node.functionName;
            useGlobalVar.put(node.functionName, new HashSet<>());
        }
        funcDef.pushBlock(new Block("entry"));
        if (!node.type.isVoid()) {
            funcDef.push(new Alloca(node.type, "%.returnValue"));
            funcDef.push(new Store(node.type, 0, "%.returnValue"));
        }
        now = funcDef;
        funcDef.irType = new IRType(node.type);
        for (int i = 0; i < node.parameterNameList.size(); ++i) {
            funcDef.pushPara(node.parameterTypeList.get(i));
            var varName = var(node.parameterNameList.get(i), node.parameterTypeList.get(i).position.line,
                    node.parameterTypeList.get(i).position.column);
            funcDef.push(new Alloca(node.parameterTypeList.get(i), varName));
            funcDef.push(new Store(node.parameterTypeList.get(i), anonymousVariable(anonymousVar++), varName));
        }
        node.body.accept(this);
        if (node.scope.notReturn) {
            funcDef.push(new Br("%returnLabel", funcDef));
        }
        funcDef.pushBlock(new Block("returnLabel"));
        if (!node.type.isVoid()) {
            funcDef.push(new Load(node.type, anonymousVariable(anonymousVar), "%.returnValue"));
            funcDef.push(new Ret(node.type, anonymousVariable(anonymousVar)));
        } else {
            funcDef.push(new Ret());
        }
        now = irProgram;
        irProgram.push(funcDef);
    }

    @Override
    public void visit(VariableDef node) {
        node.initVariablelist.forEach(varDef -> varDef.accept(this));
    }

    @Override
    public void visit(InitVariable node) {
        var tmpNow = now;
        if (node.scope.isGlobal && !inlineGlobalVar.contains(node.variableName)) {
            GlobalVarDef globalVarDef = new GlobalVarDef();
            irProgram.push(globalVarDef);
            globalVarDef.varName = "@" + node.variableName;
            globalVarDef.irType = new IRType(node.type);
            if (node.exp != null) {
                globalVarDef.setFuncDef();
                globalVarDef.funcDef.pushBlock(new Block("entry"));
                anonymousVar = 0;
                now = new Exp(globalVarDef.funcDef);
                node.exp.accept(this);
                if (((Exp) now).isOperandConst()) {
                    globalVarDef.funcDef = null;
                    globalVarDef.value = ((Exp) now).pop().varValue;
                } else {
                    globalVarDef.funcDef.push(
                            new Store(node.type, ((Exp) now).pop().varName, "@" + node.variableName));
                    globalVarDef.funcDef.push(new Ret());
                    funcMain.initList.add(new Call("@.init-" + node.variableName));
                    irProgram.push(globalVarDef.funcDef);
                }
            } else {
                globalVarDef.value = 0;
            }
        } else {
            FuncDef funcNow;
            if (node.scope.isGlobal) {
                funcNow = funcMain;
                anonymousVar = anonymousMain;
            } else {
                funcNow = (FuncDef) now;
            }
            funcNow.push(new Alloca(node.type, var(node.variableName, node.position.line, node.position.column)));
            if (node.exp != null) {
                Exp exp = new Exp(funcNow);
                now = exp;
                node.exp.accept(this);
                funcNow.push(new Store(node.type, exp.pop(), var(node.variableName, node.position.line, node.position.column)));
            } else {
                funcNow.push(new Store(node.type, 0L, var(node.variableName, node.position.line, node.position.column)));
            }
            if (node.scope.isGlobal) {
                anonymousMain = anonymousVar;
            }
        }
        now = tmpNow;
    }

    @Override
    public void visit(Suite node) {
        var tmpIf = ((FuncDef) now).getIf();
        var tmpLoop = ((FuncDef) now).getLoop();
        for (var stmt : node.statementList) {
            stmt.accept(this);
            if (tmpIf != null && ((FuncDef) now).isIf()) {
                if (tmpIf.onTrue) {
                    if (tmpIf.trueJump) {
                        break;
                    }
                } else {
                    if (tmpIf.falseJump) {
                        break;
                    }
                }
            }
            if (tmpLoop != null && !((FuncDef) now).isIf()) {
                if (tmpLoop.jump) {
                    break;
                }
            }
            if (!((FuncDef) now).notReturn) {
                break;
            }
        }
    }

    @Override
    public void visit(Statement node) {
        if (node == null) {
            return;
        }
        if (node.variableDef != null) {
            node.variableDef.accept(this);
        } else if (node.suite != null) {
            node.suite.accept(this);
        } else if (node.jumpStatement != null) {
            node.jumpStatement.accept(this);
        } else if (node.loopStatement != null) {
            node.loopStatement.accept(this);
        } else if (node.selectStatement != null) {
            node.selectStatement.accept(this);
        } else if (node.parallelExp != null) {
            node.parallelExp.accept(this);
        }
    }

    @Override
    public void visit(SelectStatement node) {
        var nowTmp = now;
        Exp exp = new Exp((FuncDef) now);
        now = exp;
        node.judgeExp.accept(this);
        now = nowTmp;
        if (exp.isOperandConst()) {
            if (exp.pop().varValue == 1) {
                node.trueStmt.accept(this);
            } else {
                if (node.falseStmt != null) {
                    node.falseStmt.accept(this);
                }
            }
        } else {
            ((FuncDef) now).pushIf();
            var tmpIf = ((FuncDef) now).getIf();
            int tmp = anonymousLabel++;
            if (node.falseStmt == null ||
                    (node.falseStmt.suite != null && node.falseStmt.suite.statementList.size() == 0)) {
                ((FuncDef) now).push(new Br(exp.pop().varName, "%trueLabel-" + tmp, "%toLabel-" + tmp, (FuncDef) now));
                ((FuncDef) now).pushBlock(new Block("trueLabel-" + tmp));
                node.trueStmt.accept(this);
                if (!tmpIf.trueJump) {
                    ((FuncDef) now).push(new Br("%toLabel-" + tmp, (FuncDef) now));
                }
                ((FuncDef) now).pushBlock(new Block("toLabel-" + tmp));
                ((FuncDef) now).popIf();
            } else {
                ((FuncDef) now).push(new Br(exp.pop().varName, "%trueLabel-" + tmp, "%falseLabel-" + tmp, (FuncDef) now));
                ((FuncDef) now).pushBlock(new Block("trueLabel-" + tmp));
                node.trueStmt.accept(this);
                if (!tmpIf.trueJump) {
                    ((FuncDef) now).push(new Br("%toLabel-" + tmp, (FuncDef) now));
                }
                tmpIf.onTrue = false;
                ((FuncDef) now).pushBlock(new Block("falseLabel-" + tmp));
                node.falseStmt.accept(this);
                if (!tmpIf.falseJump) {
                    ((FuncDef) now).push(new Br("%toLabel-" + tmp, (FuncDef) now));
                }
                if (!tmpIf.trueJump || !tmpIf.falseJump) {
                    ((FuncDef) now).pushBlock(new Block("toLabel-" + tmp));
                }
                ((FuncDef) now).popIf();
                if (((FuncDef) now).getLoop() != null && !((FuncDef) now).isIf()) {
                    var tmpLoop = ((FuncDef) now).getLoop();
                    if (tmpIf.trueJump && tmpIf.falseJump) {
                        tmpLoop.jump = true;
                    }
                } else if (((FuncDef) now).getIf() != null && ((FuncDef) now).isIf()) {
                    var tmpIfPre = ((FuncDef) now).getIf();
                    if (tmpIf.trueJump && tmpIf.falseJump) {
                        if (tmpIfPre.onTrue) {
                            tmpIfPre.trueJump = true;
                        } else {
                            tmpIfPre.falseJump = true;
                        }
                    }
                    if (!tmpIf.trueNotReturn && !tmpIf.falseNotReturn) {
                        if (tmpIfPre.onTrue) {
                            tmpIfPre.trueNotReturn = false;
                        } else {
                            tmpIfPre.falseNotReturn = false;
                        }
                    }
                } else {
                    if (!tmpIf.trueNotReturn && !tmpIf.falseNotReturn) {
                        ((FuncDef) now).notReturn = false;
                    }
                }
            }
        }
    }

    @Override
    public void visit(ForLoop node) {
//        Domain d = new Domain();
//        boolean f = d.getLoop(node);
//        Model m = new Model(d);
//        f = m.setDependency();
        ((FuncDef) now).pushLoop();
        var tmp = ((FuncDef) now).getLoop();
        String condition = var("loopCondition", node.scope.loopPos.line, node.scope.loopPos.column);
        String body = var("loopBody", node.scope.loopPos.line, node.scope.loopPos.column);
        String step = var("loopStep", node.scope.loopPos.line, node.scope.loopPos.column);
        String to = var("loopTo", node.scope.loopPos.line, node.scope.loopPos.column);
        if (node.variableDef != null) {
            node.variableDef.accept(this);
        }
        if (node.parallelExp != null) {
            node.parallelExp.accept(this);
        }
        Br br = new Br(condition, (FuncDef) now);
        ((FuncDef) now).push(br);
        ((FuncDef) now).pushBlock(new Block(condition.substring(1)));
        if (node.conditionExp != null) {
            Exp exp = new Exp((FuncDef) now);
            var nowTmp = now;
            now = exp;
            node.conditionExp.accept(this);
            now = nowTmp;
            if (exp.isOperandConst()) {
                if (exp.pop().varValue == 1) {
                    ((FuncDef) now).push(new Br(body, (FuncDef) now));
                } else {
                    ((FuncDef) now).push(new Br(to, (FuncDef) now));
                }
            } else {
                ((FuncDef) now).push(new Br(exp.pop().varName, body, to, (FuncDef) now));
            }
        } else {
            ((FuncDef) now).push(new Br(body, (FuncDef) now));
        }
        ((FuncDef) now).pushBlock(new Block(body.substring(1)));
        if (node.stmt != null) {
            node.stmt.accept(this);
        }
        if (!tmp.jump) {
            ((FuncDef) now).push(new Br(step, (FuncDef) now));
        }
        ((FuncDef) now).pushBlock(new Block(step.substring(1)));
        if (node.stepExp != null) {
            Exp exp = new Exp((FuncDef) now);
            var nowTmp = now;
            now = exp;
            node.stepExp.accept(this);
            now = nowTmp;
        }
        ((FuncDef) now).push(new Br(condition, (FuncDef) now));
        ((FuncDef) now).pushBlock(new Block(to.substring(1)));
        ((FuncDef) now).popLoop();
    }

    @Override
    public void visit(WhileLoop node) {
        ForLoop forLoop = new ForLoop();
        forLoop.conditionExp = node.judgeExp;
        forLoop.stmt = node.stmt;
        forLoop.scope = node.scope;
        forLoop.position = node.position;
        forLoop.accept(this);
    }

    @Override
    public void visit(BreakStmt node) {
        ((FuncDef) now).push(new Br(var("loopTo", node.scope.loopPos.line, node.scope.loopPos.column), (FuncDef) now));
        if (((FuncDef) now).getLoop() != null && !((FuncDef) now).isIf()) {
            ((FuncDef) now).getLoop().jump = true;
        } else if (((FuncDef) now).getIf() != null && ((FuncDef) now).isIf()) {
            var tmp = ((FuncDef) now).getIf();
            if (tmp.onTrue) {
                tmp.trueJump = true;
            } else {
                tmp.falseJump = true;
            }
        }
    }

    @Override
    public void visit(ContinueStmt node) {
        ((FuncDef) now).push(new Br(var("loopStep", node.scope.loopPos.line, node.scope.loopPos.column), (FuncDef) now));
        if (((FuncDef) now).getLoop() != null && !((FuncDef) now).isIf()) {
            ((FuncDef) now).getLoop().jump = true;
        } else if (((FuncDef) now).getIf() != null && ((FuncDef) now).isIf()) {
            var tmp = ((FuncDef) now).getIf();
            if (tmp.onTrue) {
                tmp.trueJump = true;
            } else {
                tmp.falseJump = true;
            }
        }
    }

    @Override
    public void visit(ReturnStmt node) {
        if (node.returnExp != null) {
            var nowTmp = now;
            Exp exp = new Exp((FuncDef) now);
            now = exp;
            node.returnExp.accept(this);
            now = nowTmp;
            Store store;
            store = new Store(node.returnExp.type, exp.pop(), "%.returnValue");
            ((FuncDef) now).push(store);
        }
        ((FuncDef) now).push(new Br("%returnLabel", (FuncDef) now));
        if (((FuncDef) now).getLoop() != null && !((FuncDef) now).isIf()) {
            ((FuncDef) now).getLoop().jump = true;
        } else if (((FuncDef) now).getIf() != null && ((FuncDef) now).isIf()) {
            var tmp = ((FuncDef) now).getIf();
            if (tmp.onTrue) {
                tmp.trueJump = true;
                tmp.trueNotReturn = false;
            } else {
                tmp.falseJump = true;
                tmp.falseNotReturn = false;
            }
        } else {
            ((FuncDef) now).notReturn = false;
        }
    }

    @Override
    public void visit(ParallelExp node) {
        var nowTmp = now;
        now = new Exp((FuncDef) now);
        node.expList.forEach(expression -> expression.accept(this));
        now = nowTmp;
    }

    @Override
    public void visit(ClassMemberLhsExp node) {
        node.classVariable.accept(this);
        ((Exp) now).push(new Getelementptr(anonymousVariable(anonymousVar),
                new IRType().setClass(node.classVariable.type.typeName), ((Exp) now).pop().varName, 0,
                globalScope.getClassMemberId(node.classVariable.type.typeName, node.memberName)));
        ((Exp) now).lhsVar = anonymousVariable(anonymousVar);
        ((Exp) now).push(new Load(new IRType(node.type), anonymousVariable(anonymousVar + 1), anonymousVariable(anonymousVar)));
        ++anonymousVar;
        ((Exp) now).set(anonymousVariable(anonymousVar++));
    }

    @Override
    public void visit(ClassMemFunctionLhsExp node) {
        node.classVariable.accept(this);
        Call call = null;
        if (node.classVariable.type.isString()) {
            call = new Call("@string." + node.memberFuncName);
            call.set(typePtr, ((Exp) now).pop().varName);
            if (node.callList != null) {
                for (var para : node.callList.expList) {
                    para.accept(this);
                    call.set(para.type, ((Exp) now).pop());
                }
            }
        } else if (node.classVariable.type.isArray()) {
            call = new Call("@array.size");
            call.set(typePtr, ((Exp) now).pop().varName);
        } else {
            call = new Call("@" + node.classVariable.type.typeName + "." + node.memberFuncName);
            String classVar = ((Exp) now).pop().varName;
            call.set(typePtr, classVar);
            if (node.callList != null) {
                for (var para : node.callList.expList) {
                    para.accept(this);
                    call.set(para.type, ((Exp) now).pop());
                }
            }
        }
        call.irType = new IRType(node.type);
        if (!node.type.isVoid()) {
            call.resultVar = anonymousVariable(anonymousVar);
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        }
        ((Exp) now).push(call);
    }

    @Override
    public void visit(FunctionCallLhsExp node) {
        Call call = null;
        boolean isMethod = node.scope.isClass &&
                globalScope.getClassMethod(node.scope.classType.typeName, node.functionName) != null;
        if (isMethod) {
            call = new Call("@" + node.scope.classType.typeName + "." + node.functionName);
        } else {
            call = new Call("@" + node.functionName);
        }
        if (isMethod) {
            call.set(typePtr, "%this");
        }
        if (node.callExpList != null) {
            for (var para : node.callExpList.expList) {
                para.accept(this);
                call.set(para.type, ((Exp) now).pop());
            }
        }
        call.irType = new IRType(node.type);
        if (!node.type.isVoid()) {
            call.resultVar = anonymousVariable(anonymousVar);
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        }
        ((Exp) now).push(call);
    }

    @Override
    public void visit(ArrayElementLhsExp node) {
        node.index.accept(this);
        node.variable.accept(this);
        ((Exp) now).pop();
        anonymousVar -= ((Exp) now).funcDef.pop();
        ((Exp) now).push(new Load(new IRType(node.variable.type), anonymousVariable(anonymousVar), ((Exp) now).lhsVar));
        Getelementptr getelementptr = null;
        getelementptr = new Getelementptr(anonymousVariable(anonymousVar + 1), new IRType(node.type),
                anonymousVariable(anonymousVar), -1, ((Exp) now).pop());
        ++anonymousVar;
        ((Exp) now).push(getelementptr);
        ((Exp) now).lhsVar = anonymousVariable(anonymousVar);
        ((Exp) now).push(new Load(node.type, anonymousVariable(anonymousVar + 1), anonymousVariable(anonymousVar)));
        ++anonymousVar;
        ((Exp) now).set(anonymousVariable(anonymousVar++));
    }

    @Override
    public void visit(AssignExp node) {
        node.rhs.accept(this);
        node.lhs.accept(this);
        ((Exp) now).pop();
        anonymousVar -= ((Exp) now).funcDef.pop();
        ((Exp) now).push(new Store(node.rhs.type, ((Exp) now).pop(), ((Exp) now).lhsVar));
    }

    @Override
    public void visit(BinaryExp node) {
        if (Objects.equals(node.op, "&&")) {
            node.lhs.accept(this);
            if (((Exp) now).isOperandConst()) {
                boolean lhsIsTrue = ((Exp) now).pop().varValue == 1;
                if (lhsIsTrue) {
                    node.rhs.accept(this);
                } else {
                    ((Exp) now).set(0);
                }
            } else {
                String nowLabel = ((Exp) now).funcDef.nowLabel();
                String andRhsLabel = "%andRhs-" + anonymousLabel;
                String andToLabel = "%andTo-" + anonymousLabel++;
                ((Exp) now).push(new Br(((Exp) now).pop().varName, andRhsLabel, andToLabel, ((Exp) now).funcDef));
                ((Exp) now).pushBlock(new Block(andRhsLabel.substring(1)));
                node.rhs.accept(this);
                String rhsNowLabel = ((Exp) now).funcDef.nowLabel();
                ((Exp) now).push(new Br(andToLabel, ((Exp) now).funcDef));
                ((Exp) now).pushBlock(new Block(andToLabel.substring(1)));
                Phi phi = new Phi(typeI1, anonymousVariable(anonymousVar));
                phi.push(0, nowLabel);
                phi.push(((Exp) now).pop(), rhsNowLabel);
                ((Exp) now).push(phi);
                ((Exp) now).set(anonymousVariable(anonymousVar++));
            }
            return;
        } else if (Objects.equals(node.op, "||")) {
            node.lhs.accept(this);
            if (((Exp) now).isOperandConst()) {
                boolean lhsIsTrue = ((Exp) now).pop().varValue == 1;
                if (lhsIsTrue) {
                    ((Exp) now).set(1);
                } else {
                    node.rhs.accept(this);
                }
            } else {
                String nowLabel = ((Exp) now).funcDef.nowLabel();
                String orRhsLabel = "%orRhs-" + anonymousLabel;
                String orToLabel = "%orTo-" + anonymousLabel++;
                ((Exp) now).push(new Br(((Exp) now).pop().varName, orToLabel, orRhsLabel, ((Exp) now).funcDef));
                ((Exp) now).pushBlock(new Block(orRhsLabel.substring(1)));
                node.rhs.accept(this);
                String rhsNowLabel = ((Exp) now).funcDef.nowLabel();
                ((Exp) now).push(new Br(orToLabel, ((Exp) now).funcDef));
                ((Exp) now).pushBlock(new Block(orToLabel.substring(1)));
                Phi phi = new Phi(typeI1, anonymousVariable(anonymousVar));
                phi.push(1, nowLabel);
                phi.push(((Exp) now).pop(), rhsNowLabel);
                ((Exp) now).push(phi);
                ((Exp) now).set(anonymousVariable(anonymousVar++));
            }
            return;
        }
        node.lhs.accept(this);
        node.rhs.accept(this);
        if (((Exp) now).isOperandTwoConst()) {
            long rhs = ((Exp) now).pop().varValue, lhs = ((Exp) now).pop().varValue;
            try {
                switch (node.op) {
                    case "+" -> ((Exp) now).set(lhs + rhs);
                    case "-" -> ((Exp) now).set(lhs - rhs);
                    case "*" -> ((Exp) now).set(lhs * rhs);
                    case "/" -> ((Exp) now).set(lhs / rhs);
                    case "%" -> ((Exp) now).set(lhs % rhs);
                    case "<" -> ((Exp) now).set(lhs < rhs);
                    case ">" -> ((Exp) now).set(lhs > rhs);
                    case "<=" -> ((Exp) now).set(lhs <= rhs);
                    case ">=" -> ((Exp) now).set(lhs >= rhs);
                    case "<<" -> ((Exp) now).set(lhs << rhs);
                    case ">>" -> ((Exp) now).set(lhs >> rhs);
                    case "==" -> ((Exp) now).set(lhs == rhs);
                    case "!=" -> ((Exp) now).set(lhs != rhs);
                    case "^" -> ((Exp) now).set(lhs ^ rhs);
                    case "&" -> ((Exp) now).set(lhs & rhs);
                    case "|" -> ((Exp) now).set(lhs | rhs);
                }
            } catch (Exception exception) {//直接计算会抛出错误(除以0，位移负数等)
                Binary binary = new Binary(node.op);
                binary.set(rhs);
                binary.set(lhs);
                binary.output = anonymousVariable(anonymousVar);
                ((Exp) now).set(anonymousVariable(anonymousVar++));
                ((Exp) now).push(binary);
            }
        } else {
            if (node.lhs.type.isString()) {
                Call call = null;
                switch (node.op) {
                    case "+" -> call = new Call("@string.add");
                    case "<" -> call = new Call("@string.less");
                    case ">" -> call = new Call("@string.greater");
                    case "<=" -> call = new Call("@string.lessOrEqual");
                    case ">=" -> call = new Call("@string.greaterOrEqual");
                    case "==" -> call = new Call("@string.equal");
                    case "!=" -> call = new Call("@string.notEqual");
                }
                String tmp = ((Exp) now).pop().varName;
                call.set(typePtr, ((Exp) now).pop().varName);
                call.set(typePtr, tmp);
                call.irType = new IRType(node.type);
                call.resultVar = anonymousVariable(anonymousVar);
                ((Exp) now).push(call);
                ((Exp) now).set(anonymousVariable(anonymousVar++));
                return;
            }
            switch (node.op) {
                case "+", "-", "*", "/", "%", "<<", ">>", "&", "|", "^" -> {
                    Binary binary = new Binary(node.op);
                    binary.set(((Exp) now).pop());
                    binary.set(((Exp) now).pop());
                    binary.output = anonymousVariable(anonymousVar);
                    ((Exp) now).set(anonymousVariable(anonymousVar++));
                    ((Exp) now).push(binary);
                }
                case "<", ">", "<=", ">=", "==", "!=" -> {
                    Icmp icmp = new Icmp(node.op, node.lhs.type);
                    icmp.set(((Exp) now).pop());
                    icmp.set(((Exp) now).pop());
                    icmp.output = anonymousVariable(anonymousVar);
                    ((Exp) now).set(anonymousVariable(anonymousVar++));
                    ((Exp) now).push(icmp);
                }
            }
        }
    }

    @Override
    public void visit(NewArrayExp node) {
        for (int i = node.expressionList.size() - 1; i >= 0; --i) {
            node.expressionList.get(i).accept(this);
        }
        String newPtr = newArray(node.expressionList.size());
        for (int i = 0; i < node.expressionList.size(); ++i) {
            ((Exp) now).pop();
        }
        ((Exp) now).set(newPtr);
    }

    public String newArray(int indexDim) {
        String newPtr = null;
        Call call = new Call("@.newArray");
        Cell cell = null;
        if (indexDim == 1) {
            call.set(typeI32, cell = ((Exp) now).pop());
            call.irType = typePtr;
            newPtr = call.resultVar = anonymousVariable(anonymousVar++);
            ((Exp) now).push(call);
        } else {
            call.set(typeI32, cell = ((Exp) now).pop());
            call.irType = typePtr;
            newPtr = call.resultVar = anonymousVariable(anonymousVar++);
            ((Exp) now).push(call);
            String loopVar = anonymousVariable(anonymousVar++);
            String condition = "%newArrayCondition-" + anonymousLabel;
            String body = "%newArrayBody-" + anonymousLabel;
            String to = "%newArray-To-" + anonymousLabel++;
            String nowLabel = ((Exp) now).funcDef.nowLabel();
            ((Exp) now).push(new Br(condition, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(condition.substring(1)));
            Phi phi = new Phi(typeI32, loopVar);
            ((Exp) now).push(phi);
            phi.push(0, nowLabel);
            Icmp icmp = new Icmp("<", typeI32);
            icmp.left.set(loopVar);
            icmp.right.set(cell);
            icmp.output = anonymousVariable(anonymousVar);
            ((Exp) now).push(icmp);
            ((Exp) now).push(new Br(anonymousVariable(anonymousVar++), body, to, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(body.substring(1)));
            String subNewPtr = newArray(indexDim - 1);
            ((Exp) now).push(new Getelementptr(anonymousVariable(anonymousVar),
                    typePtr, newPtr, -1, loopVar));
            ((Exp) now).push(new Store(typePtr, subNewPtr, anonymousVariable(anonymousVar++)));
            Binary binary = new Binary("+");
            binary.left.set(loopVar);
            binary.right.set(1);
            binary.output = anonymousVariable(anonymousVar);
            ((Exp) now).push(binary);
            phi.push(anonymousVariable(anonymousVar++), ((Exp) now).funcDef.nowLabel());
            ((Exp) now).push(new Br(condition, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(to.substring(1)));
        }
        ((Exp) now).set(cell);
        return newPtr;
    }

    @Override
    public void visit(NewClassExp node) {
        Call call = new Call("@.init-class-" + node.type.typeName);
        call.irType = typePtr;
        call.resultVar = anonymousVariable(anonymousVar);
        ((Exp) now).push(call);
        ((Exp) now).set(anonymousVariable(anonymousVar++));
    }

    @Override
    public void visit(PostfixExp node) {
        node.exp.accept(this);
        Binary binary;
        if (Objects.equals(node.op, "++")) {
            binary = new Binary("+");
        } else {
            binary = new Binary("-");
        }
        Type type = new Type();
        type.setInt();
        binary.set(1);
        binary.set(((Exp) now).getVar());
        binary.output = anonymousVariable(anonymousVar);
        ((Exp) now).push(binary);
        ((Exp) now).push(new Store(type, anonymousVariable(anonymousVar++), ((Exp) now).lhsVar));
    }

    @Override
    public void visit(PrefixLhsExp node) {
        node.exp.accept(this);
        Binary binary = null;
        if (Objects.equals(node.op, "++")) {
            binary = new Binary("+");
        } else {
            binary = new Binary("-");
        }
        Type type = new Type();
        type.setInt();
        binary.set(1);
        binary.set(((Exp) now).pop());
        binary.output = anonymousVariable(anonymousVar);
        ((Exp) now).set(anonymousVariable(anonymousVar));
        ((Exp) now).push(binary);
        ((Exp) now).push(new Store(type, anonymousVariable(anonymousVar++), ((Exp) now).lhsVar));
    }

    @Override
    public void visit(TernaryExp node) {
        node.condition.accept(this);
        if (((Exp) now).isOperandConst()) {
            boolean condition = (((Exp) now).pop().varValue == 1);
            if (condition) {
                node.trueExp.accept(this);
            } else {
                node.falseExp.accept(this);
            }
        } else {
            String trueLabel = "%trueLabel-" + anonymousLabel;
            String falseLabel = "%falseLabel-" + anonymousLabel;
            String toLabel = "%toLabel-" + anonymousLabel++;
            ((Exp) now).push(new Br(((Exp) now).pop().varName, trueLabel, falseLabel, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(trueLabel.substring(1)));
            node.trueExp.accept(this);
            String trueNowLabel = ((Exp) now).funcDef.nowLabel();
            ((Exp) now).push(new Br(toLabel, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(falseLabel.substring(1)));
            node.falseExp.accept(this);
            String falseNowLabel = ((Exp) now).funcDef.nowLabel();
            ((Exp) now).push(new Br(toLabel, ((Exp) now).funcDef));
            ((Exp) now).pushBlock(new Block(toLabel.substring(1)));
            Phi phi = new Phi(new IRType(node.type), anonymousVariable(anonymousVar));
            if (!node.type.isVoid()) {
                phi.push(((Exp) now).pop(), falseNowLabel);
                phi.push(((Exp) now).pop(), trueNowLabel);
                ((Exp) now).push(phi);
                ((Exp) now).set(anonymousVariable(anonymousVar++));
            }
        }
    }

    @Override
    public void visit(UnaryExp node) {
        node.exp.accept(this);
        if (Objects.equals(node.op, "!")) {
            Icmp icmp = null;
            if (((Exp) now).isOperandConst()) {
                ((Exp) now).set(((Exp) now).pop().varValue == 0);
                return;
            } else {
                icmp = new Icmp("==", typeI1);
                icmp.set(((Exp) now).pop().varName);
            }
            icmp.set(0);
            icmp.output = anonymousVariable(anonymousVar);
            ((Exp) now).push(icmp);
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        } else if (Objects.equals(node.op, "~")) {
            Binary binary = null;
            if (((Exp) now).isOperandConst()) {
                ((Exp) now).set(~((Exp) now).pop().varValue);
                return;
            } else {
                binary = new Binary("^");
                binary.set(((Exp) now).pop().varName);
            }
            binary.set(-1);
            binary.output = anonymousVariable(anonymousVar);
            ((Exp) now).push(binary);
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        } else if (Objects.equals(node.op, "-")) {
            Binary binary = null;
            if (((Exp) now).isOperandConst()) {
                ((Exp) now).set(-((Exp) now).pop().varValue);
                return;
            } else {
                binary = new Binary("-");
                binary.set(((Exp) now).pop().varName);
            }
            binary.set(0);
            binary.output = anonymousVariable(anonymousVar);
            ((Exp) now).push(binary);
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        }
    }

    @Override
    public void visit(VariableLhsExp node) {
        if (node.scope.isClass && node.id >= 0) {
            ((Exp) now).push(new Getelementptr(anonymousVariable(anonymousVar), new IRType().setClass(node.scope.classType.typeName),
                    "%this", 0, node.id));
            ((Exp) now).lhsVar = anonymousVariable(anonymousVar);
            ((Exp) now).push(new Load(new IRType(node.type), "%_" + (anonymousVar + 1), anonymousVariable(anonymousVar)));
            ++anonymousVar;
            ((Exp) now).set(anonymousVariable(anonymousVar++));
        } else {
            ((Exp) now).set(anonymousVariable(anonymousVar));
            ((Exp) now).push(new Load(node.type, anonymousVariable(anonymousVar++),
                    var(node.variableName, node.line, node.column)));
            ((Exp) now).lhsVar = var(node.variableName, node.line, node.column);
        }
        if (node.scope.isFunction && node.column == 0 && node.line == 0) {
            useGlobalVar.get(((Exp) now).funcDef.functionName.substring(1)).add(node.variableName);
        }
    }

    @Override
    public void visit(ThisPointerExp node) {
        ((Exp) now).set("%this");
    }

    @Override
    public void visit(BoolExp node) {
        ((Exp) now).set(node.value ? 1 : 0);
    }

    @Override
    public void visit(NumberExp node) {
        ((Exp) now).set(node.value);
    }

    @Override
    public void visit(StringExp node) {
        ((Exp) now).set(irProgram.pushConstString(node.value));
    }

    @Override
    public void visit(NullExp node) {
        ((Exp) now).set(0);
    }

    public String var(String varName, int line, int column) {
        if (line == 0 && column == 0 && !inlineGlobalVar.contains(varName)) {
            return "@" + varName;
        } else {
            return "%" + varName + "-" + line + "-" + column;
        }
    }

    public String anonymousVariable(int anonymousVar_) {
        return "%_" + anonymousVar_;
    }

    public void recollect() {
        for (var stmt : irProgram.stmtList) {
            if (stmt instanceof FuncDef) {
                Block newEntry = new Block("entry");
                newEntry.instrList.addAll(((FuncDef) stmt).initList);
                newEntry.instrList.addAll(((FuncDef) stmt).allocaList);
                newEntry.instrList.addAll(((FuncDef) stmt).irList.get(0).instrList);
                ((FuncDef) stmt).irList.set(0, newEntry);
            }
        }
    }

}
