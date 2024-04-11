package src.AST.definition;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.statement.Suite;
import src.Util.type.Type;

public class Constructor extends ASTNode {
    public String className;
    public Suite suite;

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
