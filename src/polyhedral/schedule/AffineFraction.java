package src.polyhedral.schedule;

import src.polyhedral.extract.Affine;
import src.polyhedral.matrix.Fraction;

import java.util.HashMap;

import static java.lang.Math.abs;
import static src.polyhedral.matrix.Fraction.getDenominatorLCM;

public class AffineFraction {
    public HashMap<String, Fraction> coefficient;
    public Fraction bias;

    public AffineFraction() {
        coefficient = new HashMap<>();
        bias = new Fraction(0);
    }

    public AffineFraction(AffineFraction obj) {
        coefficient = new HashMap<>();
        for (var entry : obj.coefficient.entrySet()) {
            coefficient.put(entry.getKey(), new Fraction(entry.getValue()));
        }
        bias = new Fraction(obj.bias);
    }


    public AffineFraction addVarCo(String variable, Fraction coefficient_) {
        if (coefficient.containsKey(variable)) {
            coefficient.computeIfPresent(variable, (k, tmp) -> tmp.add(coefficient_));
        } else {
            coefficient.put(variable, new Fraction(coefficient_));
        }
        if (coefficient.get(variable).equal(0)) {
            coefficient.remove(variable);
        }
        return this;
    }

    public AffineFraction addBias(Fraction bias_) {
        bias = bias.add(bias_);
        return this;
    }

    public boolean isConst() {
        return coefficient.isEmpty();
    }

    public Fraction getCoe(String variable) {
        if (coefficient.containsKey(variable)) {
            return coefficient.get(variable);
        } else {
            return new Fraction(0);
        }
    }

    public AffineFraction merge(AffineFraction obj, Fraction mul) {
        for (var entry : obj.coefficient.entrySet()) {
            addVarCo(entry.getKey(), entry.getValue().mul(mul));
        }
        bias = bias.add(obj.bias.mul(mul));
        return this;
    }

    public AffineFraction mul(Fraction mul) {
        for (var entry : coefficient.entrySet()) {
            entry.setValue(entry.getValue().mul(mul));
        }
        bias = bias.mul(mul);
        return this;
    }

    public AffineFraction div(Fraction div) {
        for (var entry : coefficient.entrySet()) {
            entry.setValue(entry.getValue().mul(div));
        }
        bias = bias.div(div);
        return this;
    }

    public void remove(String varName) {
        coefficient.remove(varName);
    }

    public void rebuild(boolean isCeil) {
        long de = bias.denominator();
        for (var entry : coefficient.entrySet()) {
            de = getDenominatorLCM(de, entry.getValue().denominator());
        }
        de = abs(de);
        if (isCeil) {
            System.out.print("-((");
        } else {
            System.out.print("(");
        }
        if (isCeil) {
            System.out.print(-de * bias.numerator() / bias.denominator());
        } else {
            System.out.print(de * bias.numerator() / bias.denominator());
        }
        for (var entry : coefficient.entrySet()) {

            System.out.print(" + ");
            if (isCeil) {
                System.out.print(-de * entry.getValue().numerator() / entry.getValue().denominator());
            } else {
                System.out.print(de * entry.getValue().numerator() / entry.getValue().denominator());
            }
            System.out.print("*");
            if (entry.getKey().matches(".*f\\d+.*")) {
                System.out.print("rebuildLoopIndex_" + entry.getKey());
            } else {
                System.out.print(entry.getKey());
            }
        }
        if (isCeil) {
            System.out.print(")/");
            System.out.print(de);
            System.out.print(")");
        } else {
            System.out.print(")/");
            System.out.print(de);
        }
    }
}
