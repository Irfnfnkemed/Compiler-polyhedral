package src.AST.expression;

public class PrefixLhsExp extends Expression {
    public Expression exp;
    public String op;

    public PrefixLhsExp() {
        isAssign = true;
    }
}
