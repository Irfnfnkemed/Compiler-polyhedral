package src.polyhedral.schedule;

import src.polyhedral.matrix.Fraction;

import java.util.HashMap;

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
            coefficient.get(variable).add(coefficient_);
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
}
