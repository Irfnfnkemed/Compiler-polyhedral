package src.Util.error;

import src.Util.position.Position;

public class ParserErrors extends Errors {

    public ParserErrors(String msg, int line_, int column_) {
        super(msg);
        line = line_;
        column = column_;
    }

    public ParserErrors(String msg, Position position) {
        super(msg);
        setPosition(position);
    }

    @Override
    public String toString() {
        return "Parse error (line: " + line + ", column: " + column + ") : " + message;
    }
}
