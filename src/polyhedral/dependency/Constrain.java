package src.polyhedral.dependency;

import src.polyhedral.extract.Affine;

public class Constrain {
    public static final int EQ = 0;
    public static final int LE = 1;
    public static final int GE = 2;

    public Affine lhs;
    public Affine rhs;
    public int op;

    public Constrain(Affine lhs, Affine rhs, int op) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }
}
