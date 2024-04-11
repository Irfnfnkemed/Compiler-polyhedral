package src.AST.statement.jumpStatement;

import src.AST.ASTNode;
import src.AST.ASTVisitor;

public class JumpStatement extends ASTNode {
    @Override
    public void accept(ASTVisitor visitor) {
        if (this instanceof ReturnStmt) {
            visitor.visit((ReturnStmt) this);
        } else if (this instanceof ContinueStmt) {
            visitor.visit((ContinueStmt) this);
        } else if (this instanceof BreakStmt) {
            visitor.visit((BreakStmt) this);
        }
    }
}
