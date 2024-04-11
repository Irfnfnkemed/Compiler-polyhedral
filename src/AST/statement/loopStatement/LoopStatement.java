package src.AST.statement.loopStatement;

import src.AST.ASTNode;
import src.AST.ASTVisitor;

public class LoopStatement extends ASTNode {
    @Override
    public void accept(ASTVisitor visitor) {
        if (this instanceof ForLoop) {
            visitor.visit((ForLoop) this);
        } else if (this instanceof WhileLoop) {
            visitor.visit((WhileLoop) this);
        }
    }
}
