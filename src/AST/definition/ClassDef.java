package src.AST.definition;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.definition.variableDef.VariableDef;

import java.util.ArrayList;
import java.util.List;

public class ClassDef extends ASTNode {
    public String className;
    public List<VariableDef> variableDefList;
    public List<FunctionDef> functionDefList;
    public Constructor constructor;

    public ClassDef() {
        variableDefList = new ArrayList<>();
        functionDefList = new ArrayList<>();
    }

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
