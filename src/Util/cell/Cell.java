package src.Util.cell;

public class Cell {
    public String varName;
    public long varValue;
    public boolean isConst = true;

    public Cell set(String varName_) {
        varName = varName_;
        isConst = false;
        return this;
    }

    public Cell set(long varValue_) {
        varValue = varValue_;
        isConst = true;
        return this;
    }

    public Cell set(Cell obj) {
        varName = obj.varName;
        varValue = obj.varValue;
        isConst = obj.isConst;
        return this;
    }
}
