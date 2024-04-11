package src.AST.statement;

import src.AST.ASTNode;
import src.AST.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class Suite extends ASTNode {
    public List<Statement> statementList;

    public Suite() {
        statementList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
