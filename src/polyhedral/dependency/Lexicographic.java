package src.polyhedral.dependency;

import src.polyhedral.extract.Affine;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.max;

public class Lexicographic {
    public Dependency dependency;
    public List<List<Constrain>> constrains; // all feasible lexicographic constrains

    public Lexicographic(Dependency dependency) {
        this.constrains = new ArrayList<>();
        this.dependency = dependency;
        int dim = max(dependency.coordinatesFrom.dim, dependency.coordinatesTo.dim);
        for (int i = 0; i < dim; ++i) {
            List<Constrain> constrainTmp = new ArrayList<>();
            for (int j = 0; j < i; ++j) {
                pushEqual(constrainTmp, j);
            }
            pushLess(constrainTmp, i);
            constrains.add(constrainTmp);
            if (dependency.coordinatesFrom.stmtId.get(i) > dependency.coordinatesTo.stmtId.get(i)) {
                break;
            }
            if (dependency.coordinatesFrom.stmtId.get(i) < dependency.coordinatesTo.stmtId.get(i)) {
                List<Constrain> constrainTmp2 = new ArrayList<>();
                for (int j = 0; j <= i; ++j) {
                    pushEqual(constrainTmp2, j);
                }
                constrains.add(constrainTmp2);
                break;
            }
        }
    }

    void pushLess(List<Constrain> list, int dim) {
        Affine lhs = new Affine().addVarCo(dependency.coordinatesFrom.varName.get(dim), 1).addBias(1);
        Affine rhs = new Affine().addVarCo(dependency.coordinatesTo.varName.get(dim) + "#" + dependency.id, 1);
        Constrain constrain = new Constrain(lhs, rhs, Constrain.LE);
        list.add(constrain);
    }

    void pushEqual(List<Constrain> list, int dim) {
        Affine lhs = new Affine().addVarCo(dependency.coordinatesFrom.varName.get(dim), 1);
        Affine rhs = new Affine().addVarCo(dependency.coordinatesTo.varName.get(dim) + dependency.id, 1);
        Constrain constrain = new Constrain(lhs, rhs, Constrain.EQ);
        list.add(constrain);
    }

//    boolean checkNonEmpty() {
//
//    }
}
