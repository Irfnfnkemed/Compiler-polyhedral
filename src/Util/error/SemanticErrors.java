package src.Util.error;

import src.Util.position.Position;

public class SemanticErrors extends Errors {
    public SemanticErrors(String msg, Position position) {
        super(msg);
        setPosition(position);
    }

    @Override
    public String toString() {
        return "Semantic error (line: " + line + ", column: " + column + ") : " + message;
    }
}
