package src.AST.expression;

public class TernaryExp extends Expression {
    public Expression condition;
    public Expression trueExp, falseExp;
}
