package src.AST.expression;

import src.AST.ASTNode;
import src.AST.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class ParallelExp extends ASTNode {
    public List<Expression> expList;

    public ParallelExp() {
        expList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
