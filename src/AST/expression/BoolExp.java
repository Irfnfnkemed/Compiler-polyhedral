package src.AST.expression;

import org.antlr.v4.runtime.tree.TerminalNode;

public class BoolExp extends Expression {
    public boolean value;

    public BoolExp(TerminalNode True, TerminalNode False) {
        value = (True != null);
    }

}
