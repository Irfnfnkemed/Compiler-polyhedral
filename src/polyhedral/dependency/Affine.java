package src.polyhedral.dependency;

import java.util.HashMap;
import java.util.List;

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

    public void addVarCo(String variable, long coefficient_) {
        coefficient.merge(variable, coefficient_, Long::sum);
        if (coefficient.get(variable) == 0) {
            coefficient.remove(variable);
        }
    }

    public void addBias(long bias_) {
        bias = bias + bias_;
    }

    public void merge(Affine obj, long mul) {
        for (var entry : obj.coefficient.entrySet()) {
            coefficient.merge(entry.getKey(), entry.getValue() * mul, Long::sum);
        }
        bias = bias + obj.bias * mul;
    }

    public boolean isConst() {
        return coefficient.isEmpty();
    }
}
