package src.AST;

import src.Util.position.Position;
import src.Util.scope.Scope;

abstract public class ASTNode {
    public Position position;
    public Scope scope;

    abstract public void accept(ASTVisitor visitor);
}
