package src.Util.position;

import org.antlr.v4.runtime.ParserRuleContext;

public class Position {
    public int line;
    public int column;

    public Position(ParserRuleContext ctx) {
        line = ctx.getStart().getLine();
        column = ctx.getStart().getCharPositionInLine();
    }

    public Position(Position position) {
        line = position.line;
        column = position.column;
    }

    public Position(int line, int column) {
        this.line = line;
        this.column = column;
    }
}
