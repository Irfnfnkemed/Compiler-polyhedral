package src.AST.definition.variableDef;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.Util.type.Type;
import src.AST.expression.Expression;

public class InitVariable extends ASTNode {
    public Type type;
    public String variableName;
    public Expression exp;
    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
