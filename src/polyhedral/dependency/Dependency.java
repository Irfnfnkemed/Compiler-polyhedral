package src.polyhedral.dependency;

import src.polyhedral.extract.Affine;
import src.polyhedral.extract.Coordinates;
import src.polyhedral.extract.Index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static src.polyhedral.dependency.Constrain.EQ;

public class Dependency {

    static int cnt = 0;

    public Coordinates coordinatesFrom;
    public Coordinates coordinatesTo;
    public HashMap<String, Index> indexBound;
    public List<Constrain> constrains;
    public Lexicographic lexicographic;
    public Boolean valid = true;
    public int id;

    public Dependency(MemRW from, MemRW to, HashMap<String, Index> indexBound_) {
        coordinatesFrom = from.coordinates;
        coordinatesTo = to.coordinates;
        constrains = new ArrayList<>();
        indexBound = indexBound_;
        id = cnt++;
        for (int i = 0; i < to.addr.size(); ++i) {
            if (to.addr.get(i).coefficient.size() >= 2) {
                valid = false;
                return;
            }
            Affine lhs = new Affine(to.addr.get(i), id);
            Affine rhs = new Affine(from.addr.get(i));
            rhs.addBias(-lhs.bias);
            lhs.bias = 0;
            constrains.add(new Constrain(lhs, rhs, EQ));
        }
        lexicographic = new Lexicographic(this);
    }
}
