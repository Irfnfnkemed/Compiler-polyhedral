package src.AST.expression;

public class NumberExp extends Expression {
    public long value;

    public NumberExp(String number) {
        value = Long.parseLong(number);
    }

    public NumberExp(long num) {
        value = num;
    }
}
