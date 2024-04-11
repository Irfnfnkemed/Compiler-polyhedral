package src.AST.definition.variableDef;

import src.AST.ASTVisitor;
import src.AST.definition.Definition;
import src.Util.type.Type;

import java.util.ArrayList;
import java.util.List;

public class VariableDef extends Definition {
    public Type type;
    public List<InitVariable> initVariablelist;

    public VariableDef() {
        initVariablelist = new ArrayList<>();
    }

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
