package src.AST.definition;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.statement.Suite;

public class MainDef extends ASTNode {
    public Suite suite;

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
