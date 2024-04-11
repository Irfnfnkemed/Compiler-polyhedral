package src.AST;

import src.AST.definition.Definition;

import java.util.ArrayList;
import java.util.List;

public class Program extends ASTNode {
    public List<Definition> defList;

    Program() {
        defList = new ArrayList<>();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
