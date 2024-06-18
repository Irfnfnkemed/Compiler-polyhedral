package src.polyhedral.dependency;

import src.polyhedral.extract.Affine;
import src.polyhedral.extract.Coordinates;

import java.util.ArrayList;
import java.util.List;

import static src.polyhedral.dependency.Constrain.EQ;

public class Dependency {
    public Coordinates coordinatesFrom;
    public Coordinates coordinatesTo;
    public List<Constrain> constrains;
    public Boolean valid = true;

    public Dependency(MemRW from, MemRW to) {
        coordinatesFrom = from.coordinates;
        coordinatesTo = to.coordinates;
        constrains = new ArrayList<>();
        for (int i = 0; i < to.addr.size(); ++i) {
            if (to.addr.get(i).coefficient.size() >= 2) {
                valid = false;
                return;
            }
            Affine lhs = new Affine(to.addr.get(i));
            Affine rhs = new Affine(from.addr.get(i));
            rhs.addBias(-lhs.bias);
            lhs.bias = 0;
            constrains.add(new Constrain(lhs, rhs, EQ));
        }
    }
}
