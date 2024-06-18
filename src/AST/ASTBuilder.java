package src.AST;

import org.antlr.v4.runtime.tree.ParseTree;
import src.AST.definition.*;
import src.AST.statement.Suite;
import src.AST.statement.jumpStatement.*;
import src.AST.statement.loopStatement.*;
import src.AST.statement.selectStatement.SelectStatement;
import src.AST.statement.Statement;
import src.Util.error.ParserErrors;
import src.Util.error.SemanticErrors;
import src.Util.type.Type;
import src.AST.definition.variableDef.InitVariable;
import src.AST.definition.variableDef.VariableDef;
import src.Util.position.Position;
import src.parser.MxBaseVisitor;
import src.parser.MxParser;
import src.AST.expression.*;

import java.util.Objects;

public class ASTBuilder extends MxBaseVisitor<ASTNode> {
    public Program ASTProgram;

    public ASTBuilder(ParseTree ctx) {
        ASTProgram = (Program) visit(ctx);
    }

    @Override
    public ASTNode visitProgram(MxParser.ProgramContext ctx) {
        if (ctx == null) {
            return null;
        }
        Program program = new Program();
        program.position = new Position(ctx);
        ctx.definition().forEach(ele -> program.defList.add((Definition) visitDefinition(ele)));
        return program;
    }

    @Override
    public ASTNode visitDefinition(MxParser.DefinitionContext ctx) {
        if (ctx == null) {
            return null;
        }
        Definition definition = new Definition();
        if (ctx.functionDef() != null) {
            if (Objects.equals(ctx.functionDef().Identifier(0).getText(), "main")) {
                if (!Objects.equals(ctx.functionDef().typeName(0).getText(), "int") ||
                        ctx.functionDef().Identifier().size() > 1) {
                    throw new SemanticErrors("[Program error] Wrong definition of main function.", new Position(ctx.functionDef()));
                }
                definition.mainDef = (MainDef) visitMainDef(ctx.functionDef());
                return definition;
            }
        }
        definition.classDef = (ClassDef) visitClassTypeDef(ctx.classTypeDef());
        definition.functionDef = (FunctionDef) visitFunctionDef(ctx.functionDef());
        definition.variableDef = (VariableDef) visitVariableDef(ctx.variableDef());
        definition.position = new Position(ctx);
        return definition;
    }

    public ASTNode visitMainDef(MxParser.FunctionDefContext ctx) {
        if (ctx == null) {
            return null;
        }
        MainDef mainDef = new MainDef();
        mainDef.position = new Position(ctx);
        mainDef.suite = (Suite) visitSuite(ctx.suite());
        return mainDef;
    }

    @Override
    public ASTNode visitClassTypeDef(MxParser.ClassTypeDefContext ctx) {
        if (ctx == null) {
            return null;
        }
        ClassDef classDef = new ClassDef();
        classDef.position = new Position(ctx);
        classDef.className = ctx.Identifier().getText();
        classDef.constructor = (Constructor) visitConstructor(ctx.constructor());
        ctx.variableDef().forEach(ele -> classDef.variableDefList.add((VariableDef) visitVariableDef(ele)));
        ctx.functionDef().forEach(ele -> classDef.functionDefList.add((FunctionDef) visitFunctionDef(ele)));
        return classDef;
    }

    @Override
    public ASTNode visitConstructor(MxParser.ConstructorContext ctx) {
        if (ctx == null) {
            return null;
        }
        Constructor constructor = new Constructor();
        constructor.position = new Position(ctx);
        constructor.className = ctx.Identifier().getText();
        constructor.suite = (Suite) visitSuite(ctx.suite());
        return constructor;
    }

    @Override
    public ASTNode visitFunctionDef(MxParser.FunctionDefContext ctx) {
        if (ctx == null) {
            return null;
        }
        FunctionDef functionDef = new FunctionDef();
        functionDef.position = new Position(ctx);
        functionDef.type = visitType(ctx.typeName(0));
        functionDef.functionName = ctx.Identifier(0).getText();
        ctx.typeName().stream().skip(1).forEach(ele -> functionDef.parameterTypeList.add((Type) visitType(ele)));
        ctx.Identifier().stream().skip(1).forEach(ele -> functionDef.parameterNameList.add(ele.getText()));
        functionDef.body = (Suite) visitSuite(ctx.suite());
        return functionDef;
    }

    @Override
    public ASTNode visitVariableDef(MxParser.VariableDefContext ctx) {
        if (ctx == null) {
            return null;
        }
        VariableDef variableDef = new VariableDef();
        variableDef.position = new Position(ctx);
        variableDef.type = visitType(ctx.typeName());
        ctx.initVariable().forEach(ele -> {
            var varInit = (InitVariable) visitInitVariable(ele);
            varInit.type = new Type(variableDef.type);
            variableDef.initVariablelist.add(varInit);
        });
        return variableDef;
    }

    @Override
    public ASTNode visitInitVariable(MxParser.InitVariableContext ctx) {
        if (ctx == null) {
            return null;
        }
        InitVariable initVariable = new InitVariable();
        initVariable.position = new Position(ctx);
        initVariable.variableName = ctx.Identifier().getText();
        initVariable.exp = (Expression) visitExpression(ctx.expression());
        return initVariable;
    }

    @Override
    public ASTNode visitSuite(MxParser.SuiteContext ctx) {
        if (ctx == null) {
            return null;
        }
        Suite suite = new Suite();
        suite.position = new Position(ctx);
        ctx.statement().forEach(ele -> suite.statementList.add((Statement) visitStatement(ele)));
        return suite;
    }

    @Override
    public ASTNode visitStatement(MxParser.StatementContext ctx) {
        if (ctx == null) {
            return null;
        }
        Statement statement = new Statement();
        statement.position = new Position(ctx);
        statement.suite = (Suite) visitSuite(ctx.suite());
        statement.parallelExp = (ParallelExp) visitParallelExp(ctx.parallelExp());
        statement.jumpStatement = (JumpStatement) visitJumpStatement(ctx.jumpStatement());
        statement.loopStatement = (LoopStatement) visitLoopStatement(ctx.loopStatement());
        statement.selectStatement = (SelectStatement) visitSelectStatement(ctx.selectStatement());
        statement.variableDef = (VariableDef) visitVariableDef(ctx.variableDef());
        return statement;
    }

    @Override
    public ASTNode visitSelectStatement(MxParser.SelectStatementContext ctx) {
        if (ctx == null) {
            return null;
        }
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.position = new Position(ctx);
        selectStatement.judgeExp = (Expression) visitExpression(ctx.expression());
        selectStatement.trueStmt = (Statement) visitStatement(ctx.statement(0));
        if (ctx.statement().size() == 2) {
            selectStatement.falseStmt = (Statement) visitStatement(ctx.statement(1));
        }
        return selectStatement;
    }

    public ASTNode visitLoopStatement(MxParser.LoopStatementContext ctx) {
        if (ctx == null) {
            return null;
        }
        LoopStatement loopStatement = (LoopStatement) visit(ctx);
        loopStatement.position = new Position(ctx);
        return loopStatement;
    }

    @Override
    public ASTNode visitWhileLoop(MxParser.WhileLoopContext ctx) {
        if (ctx == null) {
            return null;
        }
        WhileLoop whileLoop = new WhileLoop();
        whileLoop.judgeExp = (Expression) visitExpression(ctx.expression());
        whileLoop.stmt = (Statement) visitStatement(ctx.statement());
        return whileLoop;
    }

    @Override
    public ASTNode visitForLoop(MxParser.ForLoopContext ctx) {
        if (ctx == null) {
            return null;
        }
        ForLoop forLoop = new ForLoop();
        forLoop.parallelExp = (ParallelExp) visitParallelExp(ctx.parallelExp());
        forLoop.variableDef = (VariableDef) visitVariableDef(ctx.variableDef());
        forLoop.conditionExp = (Expression) visitExpression(ctx.condition);
        forLoop.stepExp = (Expression) visitExpression(ctx.step);
        forLoop.stmt = (Statement) visitStatement(ctx.statement());
        return forLoop;
    }

    public ASTNode visitJumpStatement(MxParser.JumpStatementContext ctx) {
        if (ctx == null) {
            return null;
        }
        JumpStatement jumpStatement = (JumpStatement) visit(ctx);
        jumpStatement.position = new Position(ctx);
        return jumpStatement;
    }

    @Override
    public ASTNode visitReturnStmt(MxParser.ReturnStmtContext ctx) {
        if (ctx == null) {
            return null;
        }
        ReturnStmt returnStmt = new ReturnStmt();
        returnStmt.returnExp = (Expression) visitExpression(ctx.expression());
        return returnStmt;
    }

    @Override
    public ASTNode visitBreakStmt(MxParser.BreakStmtContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new BreakStmt();
    }

    @Override
    public ASTNode visitContinueStmt(MxParser.ContinueStmtContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new ContinueStmt();
    }

    @Override
    public ASTNode visitParallelExp(MxParser.ParallelExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        ParallelExp parallelExp = new ParallelExp();
        ctx.expression().forEach(ele -> parallelExp.expList.add((Expression) visitExpression(ele)));
        return parallelExp;
    }

    public ASTNode visitExpression(MxParser.ExpressionContext ctx) {
        if (ctx == null) {
            return null;
        }
        Expression expression;
        expression = (Expression) visit(ctx);
        expression.position = new Position(ctx);
        return expression;
    }

    @Override
    public ASTNode visitFunctionCallLhsExp(MxParser.FunctionCallLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        FunctionCallLhsExp functionCallLhsExp = new FunctionCallLhsExp();
        functionCallLhsExp.functionName = ctx.Identifier().getText();
        functionCallLhsExp.callExpList = (ParallelExp) visitParallelExp(ctx.parallelExp());
        return functionCallLhsExp;
    }

    @Override
    public ASTNode visitClassMemberLhsExp(MxParser.ClassMemberLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        ClassMemberLhsExp classMemberLhsExp = new ClassMemberLhsExp();
        classMemberLhsExp.classVariable = (Expression) visitExpression(ctx.expression());
        classMemberLhsExp.memberName = ctx.Identifier().getText();
        classMemberLhsExp.isAssign = true;
        return classMemberLhsExp;
    }

    @Override
    public ASTNode visitClassMemFunctionLhsExp(MxParser.ClassMemFunctionLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        ClassMemFunctionLhsExp classMemFunctionLhsExp = new ClassMemFunctionLhsExp();
        classMemFunctionLhsExp.classVariable = (Expression) visitExpression(ctx.expression());
        classMemFunctionLhsExp.memberFuncName = ctx.Identifier().getText();
        classMemFunctionLhsExp.callList = (ParallelExp) visitParallelExp(ctx.parallelExp());
        return classMemFunctionLhsExp;
    }

    @Override
    public ASTNode visitPrimaryExp(MxParser.PrimaryExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        return visitExpression(ctx.expression());
    }

    @Override
    public ASTNode visitPostfixExp(MxParser.PostfixExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        PostfixExp postfixExp = new PostfixExp();
        postfixExp.exp = (Expression) visitExpression(ctx.expression());
        postfixExp.op = ctx.op.getText();
        return postfixExp;
    }

    @Override
    public ASTNode visitPrefixLhsExp(MxParser.PrefixLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        PrefixLhsExp prefixLhsExp = new PrefixLhsExp();
        prefixLhsExp.exp = (Expression) visitExpression(ctx.expression());
        prefixLhsExp.op = ctx.op.getText();
        return prefixLhsExp;
    }

    @Override
    public ASTNode visitAssignExp(MxParser.AssignExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        AssignExp assignExp = new AssignExp();
        assignExp.lhs = (Expression) visitExpression(ctx.expression(0));
        assignExp.rhs = (Expression) visitExpression(ctx.expression(1));
        return assignExp;
    }

    @Override
    public ASTNode visitBinaryExp(MxParser.BinaryExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        BinaryExp binaryExp = new BinaryExp();
        binaryExp.lhs = (Expression) visitExpression(ctx.expression(0));
        binaryExp.rhs = (Expression) visitExpression(ctx.expression(1));
        binaryExp.op = ctx.op.getText();
        return binaryExp;
    }

    @Override
    public ASTNode visitArrayElementLhsExp(MxParser.ArrayElementLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        ArrayElementLhsExp arrayElementLhsExp = new ArrayElementLhsExp();
        arrayElementLhsExp.variable = (Expression) visitExpression(ctx.expression(0));
        arrayElementLhsExp.index = (Expression) visitExpression(ctx.expression(1));
        arrayElementLhsExp.isAssign = true;
        return arrayElementLhsExp;
    }

    @Override
    public ASTNode visitTernaryExp(MxParser.TernaryExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        TernaryExp ternaryExp = new TernaryExp();
        ternaryExp.condition = (Expression) visitExpression(ctx.expression(0));
        ternaryExp.trueExp = (Expression) visitExpression(ctx.expression(1));
        ternaryExp.falseExp = (Expression) visitExpression(ctx.expression(2));
        return ternaryExp;
    }

    @Override
    public ASTNode visitUnaryExp(MxParser.UnaryExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        UnaryExp unaryExp = new UnaryExp();
        unaryExp.exp = (Expression) visitExpression(ctx.expression());
        unaryExp.op = ctx.op.getText();
        return unaryExp;
    }

    @Override
    public ASTNode visitNewArrayExp(MxParser.NewArrayExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        NewArrayExp newArrayExp = new NewArrayExp();
        newArrayExp.baseType = visitType(ctx.typeName());
        boolean emptyBrackets = false, expExist = false;
        for (var ele : ctx.bracketsWithIndex()) {
            if (ele.expression() == null) {
                emptyBrackets = true;
            } else if (!emptyBrackets) {
                newArrayExp.expressionList.add((Expression) visitExpression(ele.expression()));
                expExist = true;
            } else {
                throw new ParserErrors("Unmatched syntax of new array.",
                        ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
            }
            if (!expExist) {
                throw new ParserErrors("Unmatched syntax of new array.",
                        ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
            }
        }
        newArrayExp.type = new Type(newArrayExp.baseType);
        newArrayExp.type.dim = newArrayExp.baseType.dim + ctx.bracketsWithIndex().size();
        return newArrayExp;
    }

    @Override
    public ASTNode visitNewClassExp(MxParser.NewClassExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        NewClassExp newClassExp = new NewClassExp();
        newClassExp.type = (Type) visitType(ctx.typeName());
        return newClassExp;
    }

    @Override
    public ASTNode visitVariableLhsExp(MxParser.VariableLhsExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new VariableLhsExp(ctx.Identifier().getText());
    }

    @Override
    public ASTNode visitThisPointerExp(MxParser.ThisPointerExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        return new ThisPointerExp();
    }

    @Override
    public ASTNode visitNumberExp(MxParser.NumberExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        NumberExp numberExp = new NumberExp(ctx.DecNumber().getText());
        numberExp.type = new Type();
        numberExp.type.setInt();
        return numberExp;
    }

    @Override
    public ASTNode visitBoolExp(MxParser.BoolExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        BoolExp boolExp = new BoolExp(ctx.TRUE(), ctx.FALSE());
        boolExp.type = new Type();
        boolExp.type.setBool();
        return boolExp;
    }

    @Override
    public ASTNode visitStringExp(MxParser.StringExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        StringExp stringExp = new StringExp();
        stringExp.value = ctx.String().getText();
        stringExp.type = new Type();
        stringExp.type.setString();
        return stringExp;
    }

    @Override
    public ASTNode visitNullExp(MxParser.NullExpContext ctx) {
        if (ctx == null) {
            return null;
        }
        NullExp nullExp = new NullExp();
        nullExp.type = new Type();
        nullExp.type.setNull();
        return nullExp;
    }

    public Type visitType(MxParser.TypeNameContext ctx) {
        if (ctx == null) {
            return null;
        }
        Type type;
        if (ctx instanceof MxParser.FoundationTypeContext) {
            type = FoundationType((MxParser.FoundationTypeContext) ctx);
        } else if (ctx instanceof MxParser.ClassTypeContext) {
            type = ClassType((MxParser.ClassTypeContext) ctx);
        } else if (ctx instanceof MxParser.ArrayTypeContext) {
            type = ArrayType((MxParser.ArrayTypeContext) ctx);
        } else {
            return null;
        }
        type.position = new Position(ctx);
        return type;
    }


    public Type FoundationType(MxParser.FoundationTypeContext ctx) {
        if (ctx == null) {
            return null;
        }
        Type type = new Type();
        if (ctx.VOID() != null) {
            return type.setVoid();
        } else if (ctx.BOOL() != null) {
            return type.setBool();
        } else if (ctx.INT() != null) {
            return type.setInt();
        } else if (ctx.STRING() != null) {
            return type.setString();
        } else {
            return null;
        }
    }

    public Type ClassType(MxParser.ClassTypeContext ctx) {
        if (ctx == null) {
            return null;
        }
        Type type = new Type();
        return type.setClass(ctx.Identifier().getText());
    }

    public Type ArrayType(MxParser.ArrayTypeContext ctx) {
        if (ctx == null) {
            return null;
        }
        Type type = new Type();
        return type.setArray(visitType(ctx.typeName()), ctx.brackets().size());
    }
}
