package src.AST.statement.selectStatement;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.statement.Statement;
import src.AST.expression.Expression;

public class SelectStatement extends ASTNode {
    public Expression judgeExp;
    public Statement trueStmt = null;
    public Statement falseStmt = null;

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
