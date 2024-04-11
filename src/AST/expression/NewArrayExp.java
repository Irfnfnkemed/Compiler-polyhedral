package src.AST.expression;

import src.Util.type.Type;

import java.util.ArrayList;
import java.util.List;

public class NewArrayExp extends Expression {
    public Type baseType;
    public List<Expression> expressionList;

    public NewArrayExp() {
        expressionList = new ArrayList<>();
        isAssign = true;
    }
}
