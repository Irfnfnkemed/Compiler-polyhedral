package src.polyhedral.extract;

import java.util.ArrayList;
import java.util.List;

public class Coordinates {
    public int dim;
    public List<String> varName;
    public List<Integer> stmtId;

    public Coordinates() {
        varName = new ArrayList<>();
        stmtId = new ArrayList<>();
        dim = 0;
    }

    public Coordinates(Coordinates obj) {
        dim = obj.dim;
        varName = new ArrayList<>(obj.varName);
        stmtId = new ArrayList<>(obj.stmtId);
    }

    public void push(Index index) {
        ++dim;
        varName.add(index.varName);
        stmtId.add(0);
    }

    public void pop() {
        --dim;
        varName.remove(dim);
        stmtId.remove(dim);
    }

    public void next() {
        if (dim > 0) {
            stmtId.set(dim - 1, stmtId.get(dim - 1) + 1);
        }
    }
}
