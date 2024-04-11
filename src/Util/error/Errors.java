package src.Util.error;

import src.Util.position.Position;

abstract public class Errors extends RuntimeException {
    public String message;
    public int line, column;

    Errors(String msg) {
        message = msg;
    }

    public void setPosition(Position position) {
        line = position.line;
        column = position.column;
    }

    abstract public String toString();
}
