package src.AST.definition;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.definition.variableDef.VariableDef;

public class Definition extends ASTNode {
    public MainDef mainDef;
    public ClassDef classDef;
    public FunctionDef functionDef;
    public VariableDef variableDef;

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
