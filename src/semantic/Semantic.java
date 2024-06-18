package src.semantic;

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
import src.Util.error.SemanticErrors;
import src.Util.scope.GlobalScope;
import src.Util.scope.Scope;
import src.Util.type.Type;

import java.util.*;

public class Semantic implements ASTVisitor {
    public GlobalScope globalScope;
    public Program ASTProgram;
    public HashSet<String> inlineGlobalVar;//仅在main函数出现的全局变量
    public HashMap<String, List<VariableLhsExp>> inlineGlobalNode;//仅在main函数出现的全局变量对应的节点
    private boolean isMain = false;//当前是否在处理main函数

    public Semantic(Program ASTProgram_) {
        globalScope = new GlobalScope();
        ASTProgram = ASTProgram_;
        inlineGlobalVar = new HashSet<>();
        inlineGlobalNode = new HashMap<>();
        for (var def : ASTProgram.defList) {
            if (def.functionDef != null) {
                globalScope.setFunction(def.functionDef.functionName, def.functionDef.type,
                        def.functionDef.parameterTypeList, def.functionDef.position);
            } else if (def.mainDef != null) {
                Type type = new Type();
                type.setInt();
                globalScope.setFunction("main", type, new ArrayList<>(), def.mainDef.position);
            } else if (def.classDef != null) {
                globalScope.setClassName(def.classDef.className, def.classDef.position);
                for (var varList : def.classDef.variableDefList) {
                    varList.initVariablelist.forEach(var -> globalScope.setClassMember(
                            def.classDef.className, var.variableName, varList.type, var.position));
                }
                def.classDef.functionDefList.forEach(funcList -> globalScope.setClassMethod(
                        def.classDef.className, funcList.functionName, funcList.type,
                        funcList.parameterTypeList, funcList.position));
            }
        }
    }

    public void check() {
        visit(ASTProgram);
    }

    public void visit(Program node) {
        boolean mainExist = false;
        //Definition mainDef = null;
        for (var def : node.defList) {
            if (def.mainDef != null) {
                if (mainExist) {
                    throw new SemanticErrors("[Program error] Duplicate definition of main function.", def.mainDef.position);
                } else {
                    mainExist = true;
                }
                //mainDef = def;
                //continue;
            }
            def.accept(this);
        }
        if (!mainExist) {
            throw new SemanticErrors("[Program error] Missing main function.", node.position);
        }
        // mainDef.accept(this);
    }

    public void visit(Definition node) {
        if (node == null) {
            return;
        }
        if (node.mainDef != null) {
            isMain = true;
            node.mainDef.scope = new Scope();
            node.mainDef.scope.isFunction = true;
            node.mainDef.scope.returnType = new Type();
            node.mainDef.scope.returnType.setInt();
            node.mainDef.accept(this);
            isMain = false;
        } else if (node.classDef != null) {
            node.classDef.scope = new Scope();
            node.classDef.scope.isClass = true;
            node.classDef.scope.classType = new Type();
            node.classDef.scope.classType.setClass(node.classDef.className);
            node.classDef.accept(this);
        } else if (node.functionDef != null) {
            node.functionDef.scope = new Scope();
            node.functionDef.scope.isFunction = true;
            node.functionDef.accept(this);
        } else if (node.variableDef != null) {
            node.variableDef.scope = new Scope();
            node.variableDef.scope.isGlobal = true;
            node.variableDef.accept(this);
        }
    }

    public void visit(MainDef node) {
        if (node == null) {
            return;
        }
        node.suite.scope = node.scope;
        node.suite.accept(this);
    }

    public void visit(ClassDef node) {
        if (node == null) {
            return;
        }
        node.variableDefList.forEach(varDef -> {
            varDef.scope = node.scope;
            varDef.accept(this);
        });
        node.functionDefList.forEach(funcDef -> {
            funcDef.scope = new Scope(node.scope);
            funcDef.scope.isFunction = true;
            funcDef.accept(this);
        });
        if (node.constructor != null) {
            node.constructor.scope = new Scope(node.scope);
            node.constructor.scope.isFunction = true;
            node.constructor.scope.isConstructor = true;
            node.constructor.scope.returnType = new Type();
            node.constructor.scope.returnType.setVoid();
            node.constructor.accept(this);
        }
    }

    public void visit(Constructor node) {
        if (node == null) {
            return;
        }
        if (!node.scope.isClass) {
            throw new SemanticErrors("[Function error] Unexpected constructor out of the class.", node.position);
        }
        if (!Objects.equals(node.className, node.scope.classType.typeName)) {
            throw new SemanticErrors("[Name error] Wrong Name of the constructor.", node.position);
        }
        node.suite.scope = node.scope;
        node.suite.accept(this);
    }

    public void visit(FunctionDef node) {
        if (node == null) {
            return;
        }
        if (node.type.isClass() && !globalScope.classIsExist(node.type.typeName)) {
            throw new SemanticErrors("[Type error] Type not define.", node.type.position);
        }
        node.scope.returnType = new Type(node.type);
        for (Type parameterType : node.parameterTypeList) {
            if (parameterType.isClass() && !globalScope.classIsExist(parameterType.typeName)) {
                throw new SemanticErrors("[Type error] Type not define.", parameterType.position);
            }
        }
        for (int i = 0; i < node.parameterNameList.size(); ++i) {
            node.scope.setVariable(node.parameterNameList.get(i), node.parameterTypeList.get(i), node.parameterTypeList.get(i).position);
        }
        node.body.scope = node.scope;
        node.body.accept(this);
        if (node.scope.notReturn && !node.scope.returnType.isVoid()) {
            throw new SemanticErrors("[Function error] Missing return statement.", node.position);
        }
    }

    public void visit(VariableDef node) {
        if (node == null) {
            return;
        }
        if (node.type.isClass() && !globalScope.classIsExist(node.type.typeName)) {
            throw new SemanticErrors("[Type error] Type not define.", node.type.position);
        }
        if (node.type.typeEnum == Type.TypeEnum.VOID || node.type.typeEnum == Type.TypeEnum.NULL) {
            throw new SemanticErrors("[Type error] Invalid type.", node.type.position);
        }
        for (var varDef : node.initVariablelist) {
            varDef.scope = node.scope;
            varDef.accept(this);
            if (node.scope.isGlobal) {
                globalScope.setVariable(varDef.variableName, varDef.type, varDef.position);
                inlineGlobalVar.add(varDef.variableName);
            } else if (!(node.scope.isClass && !node.scope.isFunction)) {
                node.scope.setVariable(varDef.variableName, varDef.type, varDef.position);
            }
        }
    }

    public void visit(InitVariable node) {
        if (node == null) {
            return;
        }
        if (node.exp != null) {
            node.exp.scope = node.scope;
            node.exp.accept(this);
            if (!node.type.assign(node.exp.type)) {
                throw new SemanticErrors("[Type error] Initialization type mismatch.", node.exp.position);
            }
        }
    }

    public void visit(Suite node) {
        if (node == null) {
            return;
        }
        for (var stmt : node.statementList) {
            stmt.scope = node.scope;
            stmt.accept(this);
        }
    }

    public void visit(Statement node) {
        if (node == null) {
            return;
        }
        if (node.variableDef != null) {
            node.variableDef.scope = node.scope;
            node.variableDef.accept(this);
        } else if (node.suite != null) {
            node.suite.scope = new Scope(node.scope);
            node.suite.accept(this);
            if (node.scope.isFunction && !node.suite.scope.notReturn) {
                node.scope.notReturn = false;
            }
        } else if (node.jumpStatement != null) {
            node.jumpStatement.scope = node.scope;
            node.jumpStatement.accept(this);
        } else if (node.loopStatement != null) {
            node.loopStatement.scope = new Scope(node.scope);
            node.loopStatement.scope.loopPos = node.loopStatement.position;
            node.loopStatement.accept(this);
            if (node.scope.isFunction && !node.loopStatement.scope.notReturn) {
                node.scope.notReturn = false;
            }
        } else if (node.selectStatement != null) {
            node.selectStatement.scope = node.scope;
            node.selectStatement.accept(this);
            if (node.scope.isFunction) {
                if (!node.selectStatement.trueStmt.scope.notReturn && node.selectStatement.falseStmt != null &&
                        !node.selectStatement.falseStmt.scope.notReturn) {
                    node.scope.notReturn = false;
                }
            }
        } else if (node.parallelExp != null) {
            node.parallelExp.scope = node.scope;
            node.parallelExp.accept(this);
        }
    }

    public void visit(SelectStatement node) {
        if (node == null) {
            return;
        }
        node.judgeExp.scope = node.scope;
        node.judgeExp.accept(this);
        if (!node.judgeExp.type.isBool()) {
            throw new SemanticErrors("[Statement error] Invalid judgment expression.", node.judgeExp.position);
        }
        node.trueStmt.scope = new Scope(node.scope);
        node.trueStmt.accept(this);
        if (node.falseStmt != null) {
            node.falseStmt.scope = new Scope(node.scope);
            node.falseStmt.accept(this);
        }
        if (!node.trueStmt.scope.notReturn && node.falseStmt != null && !node.falseStmt.scope.notReturn) {
            node.scope.notReturn = false;
        }
    }

    public void visit(ForLoop node) {
        if (node == null) {
            return;
        }
        if (node.parallelExp != null) {
            node.parallelExp.scope = node.scope;
            node.parallelExp.accept(this);
        }
        if (node.variableDef != null) {
            node.variableDef.scope = node.scope;
            node.variableDef.accept(this);
        }
        if (node.conditionExp != null) {
            node.conditionExp.scope = node.scope;
            node.conditionExp.accept(this);
            if (!node.conditionExp.type.isBool()) {
                throw new SemanticErrors("[Statement error] Invalid judgment expression.", node.conditionExp.position);
            }
        }
        if (node.stepExp != null) {
            node.stepExp.scope = node.scope;
            node.stepExp.accept(this);
        }
        node.stmt.scope = node.scope;
        node.stmt.accept(this);
    }

    public void visit(WhileLoop node) {
        if (node == null) {
            return;
        }
        node.judgeExp.scope = node.scope;
        node.judgeExp.accept(this);
        if (!node.judgeExp.type.isBool()) {
            throw new SemanticErrors("[Statement error] Invalid judgment expression.", node.judgeExp.position);
        }
        node.stmt.scope = node.scope;
        node.stmt.accept(this);
    }

    public void visit(BreakStmt node) {
        if (node.scope.loopPos == null) {
            throw new SemanticErrors("[Statement error] Unexpected break statement.", node.position);
        }
    }

    public void visit(ContinueStmt node) {
        if (node.scope.loopPos == null) {
            throw new SemanticErrors("[Statement error] Unexpected continue statement.", node.position);
        }
    }

    public void visit(ReturnStmt node) {
        if (!node.scope.isFunction) {
            throw new SemanticErrors("[Statement error] Unexpected return.", node.position);
        }
        if (node.returnExp == null) {
            if (!node.scope.returnType.isVoid()) {
                throw new SemanticErrors("[Type error] Unmatched return type.", node.position);
            }
        } else {
            node.returnExp.scope = node.scope;
            node.returnExp.accept(this);
            if (!node.scope.returnType.assign(node.returnExp.type)) {
                throw new SemanticErrors("[Type error] Unmatched return type.", node.position);
            }
        }
        node.scope.notReturn = false;
    }

    public void visit(ParallelExp node) {
        if (node == null) {
            return;
        }
        for (var exp : node.expList) {
            exp.scope = node.scope;
            exp.accept(this);
        }
    }

    public void visit(ClassMemberLhsExp node) {
        if (node == null) {
            return;
        }
        node.classVariable.scope = node.scope;
        node.classVariable.accept(this);
        if (!node.classVariable.type.isClass()) {
            throw new SemanticErrors("[Class error] Visit member of non class type variable.", node.position);
        }
        Type memberType = globalScope.getClassMember(node.classVariable.type.GetType(), node.memberName);
        if (memberType == null) {
            throw new SemanticErrors("[Class error] Absent class member.", node.position);
        }
        node.type = new Type(memberType);
        node.isAssign = true;
    }

    public void visit(ClassMemFunctionLhsExp node) {
        if (node == null) {
            return;
        }
        node.classVariable.scope = node.scope;
        node.classVariable.accept(this);
        if (!node.classVariable.type.isClass() && !node.classVariable.type.isString()) {
            if (node.classVariable.type.isArray() && Objects.equals(node.memberFuncName, "size")) {
                if (node.callList != null) {
                    throw new SemanticErrors("[Class error] Absent class function.", node.position);
                } else {
                    node.type = new Type();
                    node.type.setInt();
                    return;
                }
            } else {
                throw new SemanticErrors("[Class error] Absent class function.", node.position);
            }
        }
        var funcTypes = globalScope.getClassMethod(node.classVariable.type.GetType(), node.memberFuncName);
        if (funcTypes == null) {
            throw new SemanticErrors("[Class error] Absent class function.", node.position);
        }
        int size = 0;
        if (node.callList != null) {
            size = node.callList.expList.size();
        }
        if (funcTypes.parameterTypes.size() != size) {
            throw new SemanticErrors("[Function error] Unmatched parameter number.", node.position);
        }
        if (size != 0) {
            node.callList.scope = node.scope;
            node.callList.accept(this);
        }
        for (int i = 0; i < funcTypes.parameterTypes.size(); ++i) {
            if (!funcTypes.parameterTypes.get(i).assign(node.callList.expList.get(i).type)) {
                throw new SemanticErrors("[Type error] Unmatched parameter type.", node.callList.expList.get(i).type.position);
            }
        }
        node.type = new Type(funcTypes.type);
    }

    public void visit(FunctionCallLhsExp node) {
        if (node == null) {
            return;
        }
        GlobalScope.FunctionTypes funcTypes = null;
        if (node.scope.isClass) {
            funcTypes = globalScope.getClassMethod(node.scope.classType.GetType(), node.functionName);
        }
        if (funcTypes == null) {
            funcTypes = globalScope.getFunction(node.functionName);
        }
        if (funcTypes == null) {
            throw new SemanticErrors("[Function error] Call absent function.", node.position);
        }
        int size = 0;
        if (node.callExpList != null) {
            size = node.callExpList.expList.size();
        }
        if (funcTypes.parameterTypes.size() != size) {
            throw new SemanticErrors("[Function error] Unmatched parameter number.", node.position);
        }
        if (size != 0) {
            node.callExpList.scope = node.scope;
            node.callExpList.accept(this);
        }
        for (int i = 0; i < funcTypes.parameterTypes.size(); ++i) {
            if (!funcTypes.parameterTypes.get(i).assign(node.callExpList.expList.get(i).type)) {
                throw new SemanticErrors("[Type error] Unmatched parameter type.", node.callExpList.expList.get(i).position);
            }
        }
        node.type = new Type(funcTypes.type);
    }

    public void visit(ArrayElementLhsExp node) {
        if (node == null) {
            return;
        }
        node.variable.scope = node.scope;
        node.variable.accept(this);
        if (!node.variable.type.isArray()) {
            throw new SemanticErrors("[Type error] Non array type.", node.position);
        }
        node.index.scope = node.scope;
        node.index.accept(this);
        if (!node.index.type.isInt()) {
            throw new SemanticErrors("[Type error] Unexpected type of index.", node.index.position);
        }
        node.type = new Type(node.variable.type);
        --node.type.dim;
    }

    public void visit(AssignExp node) {
        if (node == null) {
            return;
        }
        node.lhs.scope = node.scope;
        node.lhs.accept(this);
        if (!node.lhs.isAssign) {
            throw new SemanticErrors("[Type error] Assign to right value.", node.position);
        }
        node.rhs.scope = node.scope;
        node.rhs.accept(this);
        if (!node.lhs.type.assign(node.rhs.type)) {
            throw new SemanticErrors("[Type error] Unmatched type to assign.", node.position);
        }
    }

    public void visit(BinaryExp node) {
        if (node == null) {
            return;
        }
        node.lhs.scope = node.scope;
        node.lhs.accept(this);
        node.rhs.scope = node.scope;
        node.rhs.accept(this);
        switch (node.op) {
            case "+", "<", ">", "<=", ">=" -> {
                if (node.lhs.type.isString()) {
                    if (!node.rhs.type.isString()) {
                        throw new SemanticErrors("[Type error] Unexpected type in binary expression.", node.position);
                    }
                    node.type = new Type();
                    if (Objects.equals(node.op, "+")) {
                        node.type.setString();
                    } else {
                        node.type.setBool();
                    }
                    return;
                }
                if (!node.lhs.type.isInt() || !node.rhs.type.isInt()) {
                    throw new SemanticErrors("[Type error] Unexpected type in binary expression.", node.position);
                }
                node.type = new Type();
                if (Objects.equals(node.op, "+")) {
                    node.type.setInt();
                } else {
                    node.type.setBool();
                }
            }
            case "-", "*", "/", "%", "<<", ">>", "&", "^", "|" -> {
                if (!node.lhs.type.isInt() || !node.rhs.type.isInt()) {
                    throw new SemanticErrors("[Type error] Unexpected type in binary expression.", node.position);
                }
                node.type = new Type();
                node.type.setInt();
            }
            case "==", "!=" -> {
                if (!node.lhs.type.compare(node.rhs.type)) {
                    throw new SemanticErrors("[Type error] Unexpected type in binary expression.", node.position);
                }
                node.type = new Type();
                node.type.setBool();
            }
            case "||", "&&" -> {
                if (!node.lhs.type.isBool() || !node.rhs.type.isBool()) {
                    throw new SemanticErrors("[Type error] Unexpected type in binary expression.", node.position);
                }
                node.type = new Type();
                node.type.setBool();
            }
        }
    }

    public void visit(NewArrayExp node) {
        if (node == null) {
            return;
        }
        if (node.baseType.isArray() || node.baseType.isNull() || node.baseType.isVoid()) {
            throw new SemanticErrors("[Type error] Invalid new array type.", node.position);
        }
        if (node.baseType.isClass() && !globalScope.classIsExist(node.type.GetType())) {
            throw new SemanticErrors("[Type error] Non existent type.", node.position);
        }
        node.expressionList.forEach(exp -> {
            exp.scope = node.scope;
            exp.accept(this);
            if (!exp.type.isInt()) {
                throw new SemanticErrors("[Type error] Invalid type of index.", exp.position);
            }
        });
    }

    public void visit(NewClassExp node) {
        if (node == null) {
            return;
        }
        if (node.type.isArray() || !node.type.isClass()) {
            throw new SemanticErrors("[Type error] Invalid type.", node.position);
        }
        if (!globalScope.classIsExist(node.type.GetType())) {
            throw new SemanticErrors("[Type error] Non existent type.", node.position);
        }
    }

    public void visit(PostfixExp node) {
        if (node == null) {
            return;
        }
        node.exp.scope = node.scope;
        node.exp.accept(this);
        if (!node.exp.isAssign) {
            throw new SemanticErrors("[Type error] Postfix expression on right value.", node.exp.position);
        }
        if (!node.exp.type.isInt()) {
            throw new SemanticErrors("[Type error] Invalid type in postfix expression.", node.exp.position);
        }
        node.type = new Type(node.exp.type);
    }

    public void visit(PrefixLhsExp node) {
        if (node == null) {
            return;
        }
        node.exp.scope = node.scope;
        node.exp.accept(this);
        if (!node.exp.isAssign) {
            throw new SemanticErrors("[Type error] Prefix expression on right value.", node.exp.position);
        }
        if (!node.exp.type.isInt()) {
            throw new SemanticErrors("[Type error] Invalid type in prefix expression.", node.exp.position);
        }
        node.type = new Type(node.exp.type);
    }

    public void visit(TernaryExp node) {
        node.condition.scope = node.scope;
        node.condition.accept(this);
        if (!node.condition.type.isBool()) {
            throw new SemanticErrors("[Statement error] Invalid judgement expression.", node.condition.position);
        }
        node.trueExp.scope = node.scope;
        node.trueExp.accept(this);
        node.falseExp.scope = node.scope;
        node.falseExp.accept(this);
        if (!node.trueExp.type.compare(node.falseExp.type)) {
            throw new SemanticErrors("[Type error] Unmatched type of ternary expression.", node.trueExp.position);
        }
        node.type = new Type(Type.getCommon(node.trueExp.type, node.falseExp.type));
    }

    public void visit(UnaryExp node) {
        if (node == null) {
            return;
        }
        node.exp.scope = node.scope;
        node.exp.accept(this);
        if (Objects.equals(node.op, "!")) {
            if (!node.exp.type.isBool()) {
                throw new SemanticErrors("[Type error] Invalid type in unary expression", node.exp.position);
            }
        } else {
            if (!node.exp.type.isInt()) {
                throw new SemanticErrors("[Type error] Invalid type in unary expression", node.exp.position);
            }
        }
        node.type = new Type(node.exp.type);
    }

    public void visit(VariableLhsExp node) {
        if (node == null) {
            return;
        }
        var varTmp = node.scope.getVariable(node.variableName);
        if (varTmp != null) {
            node.type = new Type(varTmp.type);
            node.line = varTmp.line;
            node.column = varTmp.column;
        } else {
            Type typeTmp = null;
            if (node.scope.isClass) {
                typeTmp = globalScope.getClassMember(node.scope.classType.GetType(), node.variableName);
                if (typeTmp != null) {
                    node.id = globalScope.getClassMemberId(node.scope.classType.GetType(), node.variableName);
                }
            }
            if (typeTmp == null) {
                varTmp = globalScope.getVariable(node.variableName);
                if (varTmp != null) {
                    typeTmp = varTmp.type;
                    if (!isMain) {
                        inlineGlobalVar.remove(node.variableName);
                        var varNodeList = inlineGlobalNode.get(node.variableName);
                        if (varNodeList != null) {
                            for (var varNode : varNodeList) {
                                varNode.line = 0;
                                varNode.column = 0;
                            }
                        }
                    } else if (inlineGlobalVar.contains(node.variableName)) {
                        node.line = varTmp.line;
                        node.column = varTmp.column;
                        inlineGlobalNode.computeIfAbsent(node.variableName, k -> new ArrayList<>()).add(node);
                    }
                }
            }
            if (typeTmp == null) {
                throw new SemanticErrors("[Name error] Not defined variable", node.position);
            }
            node.type = new Type(typeTmp);
        }
        node.isAssign = true;
    }

    public void visit(ThisPointerExp node) {
        if (node == null) {
            return;
        }
        if (!node.scope.isClass) {
            throw new SemanticErrors("[Statement] Invalid this pointer.", node.position);
        }
        node.type = new Type(node.scope.classType);
    }

    public void visit(BoolExp node) {
    }

    public void visit(NumberExp node) {
    }

    public void visit(StringExp node) {
    }

    public void visit(NullExp node) {
    }

}
