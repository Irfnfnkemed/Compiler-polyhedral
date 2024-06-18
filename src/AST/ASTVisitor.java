package src.AST;

import src.AST.definition.*;
import src.AST.definition.variableDef.*;
import src.AST.expression.*;
import src.AST.statement.*;
import src.AST.statement.jumpStatement.*;
import src.AST.statement.loopStatement.*;
import src.AST.statement.selectStatement.SelectStatement;
import src.Util.error.SemanticErrors;
import src.Util.scope.GlobalScope;
import src.Util.scope.Scope;
import src.Util.type.Type;

import java.util.Objects;

public interface ASTVisitor {
    public void visit(Program node);

    public void visit(Definition node);

    public void visit(MainDef node);

    public void visit(ClassDef node);

    public void visit(Constructor node);

    public void visit(FunctionDef node);

    public void visit(VariableDef node);

    public void visit(InitVariable node);

    public void visit(Suite node);

    public void visit(Statement node);

    public void visit(SelectStatement node);

    public void visit(ForLoop node);

    public void visit(WhileLoop node);

    public void visit(BreakStmt node);

    public void visit(ContinueStmt node);

    public void visit(ReturnStmt node);

    public void visit(ParallelExp node);

    public void visit(ClassMemberLhsExp node);

    public void visit(ClassMemFunctionLhsExp node);

    public void visit(FunctionCallLhsExp node);

    public void visit(ArrayElementLhsExp node);

    public void visit(AssignExp node);

    public void visit(BinaryExp node);

    public void visit(NewArrayExp node);

    public void visit(NewClassExp node);

    public void visit(PostfixExp node);

    public void visit(PrefixLhsExp node);

    public void visit(TernaryExp node);

    public void visit(UnaryExp node);

    public void visit(VariableLhsExp node);

    public void visit(ThisPointerExp node);

    public void visit(BoolExp node);

    public void visit(NumberExp node);

    public void visit(StringExp node);

    public void visit(NullExp node);
}
