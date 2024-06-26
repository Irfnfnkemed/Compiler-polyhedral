package src.polyhedral.extract;

import java.util.HashMap;

public class Affine {
    public HashMap<String, Long> coefficient;
    public long bias;

    public Affine() {
        coefficient = new HashMap<>();
        bias = 0;
    }

    public Affine(Affine obj) {
        coefficient = new HashMap<>();
        coefficient.putAll(obj.coefficient);
        bias = obj.bias;
    }

    public Affine(Affine obj, int cnt) {
        coefficient = new HashMap<>();
        for (var entry : obj.coefficient.entrySet()) {
            coefficient.put(entry.getKey() + "#" + cnt, entry.getValue());
        }
        bias = obj.bias;
    }

    public Affine addVarCo(String variable, long coefficient_) {
        coefficient.merge(variable, coefficient_, Long::sum);
        if (coefficient.get(variable) == 0) {
            coefficient.remove(variable);
        }
        return this;
    }

    public Affine addBias(long bias_) {
        bias = bias + bias_;
        return this;
    }

    public void merge(Affine obj, long mul) {
        for (var entry : obj.coefficient.entrySet()) {
            coefficient.merge(entry.getKey(), entry.getValue() * mul, Long::sum);
        }
        coefficient.entrySet().removeIf(entry -> entry.getValue() == 0);
        bias = bias + obj.bias * mul;
    }

    public void mul(long mul) {
        for (var entry : coefficient.entrySet()) {
            entry.setValue(entry.getValue() * mul);
        }
        bias *= mul;
    }

    public boolean isConst() {
        return coefficient.isEmpty();
    }
}
