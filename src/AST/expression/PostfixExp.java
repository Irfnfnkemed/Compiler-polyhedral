package src.AST.expression;

public class PostfixExp extends Expression {
    public Expression exp;
    public String op;

    public PostfixExp() {
        isAssign = false;
    }
}
