package src.AST.definition;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.statement.Suite;
import src.Util.type.Type;

import java.util.ArrayList;
import java.util.List;

public class FunctionDef extends ASTNode {
    public Type type;
    public String functionName;
    public List<Type> parameterTypeList;
    public List<String> parameterNameList;
    public Suite body;

    public FunctionDef() {
        parameterTypeList = new ArrayList<>();
        parameterNameList = new ArrayList<>();
    }

    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
