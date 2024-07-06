package src.polyhedral.schedule;

import src.polyhedral.extract.Index;
import src.polyhedral.matrix.Fraction;
import src.polyhedral.matrix.Matrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FourierMotzkin {
    public List<Index> indexList;
    public HashMap<String, List<AffineFraction>> lowerBound;
    public HashMap<String, List<AffineFraction>> upperBound;


    public FourierMotzkin(List<Index> indexList) {
        this.indexList = indexList;
        this.lowerBound = new HashMap<>();
        this.upperBound = new HashMap<>();

    }

    void setTransform(Matrix transformInverse) {
        for (int i = 0; i < transformInverse.row(); ++i) { // set initial constrains
            for (int j = transformInverse.row() - 1; j >= 0; --j) {
                var coe = transformInverse.getElement(i, j);
                if (!coe.equal(0)) {
                    AffineFraction affine1 = new AffineFraction();
                    var boundTo = indexList.get(i).boundTo;
                    if (!boundTo.isConst()) {
                        for (var entry : boundTo.coefficient.entrySet()) {
                            affine1.addVarCo(entry.getKey(), new Fraction(Math.toIntExact(entry.getValue())).div(coe));
                        }
                    }
                    affine1.addBias(new Fraction((int) boundTo.bias).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine1.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, coe.less(0)).add(affine1);
                    AffineFraction affine2 = new AffineFraction();
                    var boundFrom = indexList.get(i).boundFrom;
                    if (!boundFrom.isConst()) {
                        for (var entry : boundFrom.coefficient.entrySet()) {
                            affine2.addVarCo(entry.getKey(), new Fraction(Math.toIntExact(entry.getValue())).div(coe));
                        }
                    }
                    affine2.addBias(new Fraction((int) boundFrom.bias).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine2.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, !coe.less(0)).add(affine2);
                    break;
                }
            }
        }
        for (int i = transformInverse.row() - 1; i >= 0; --i) {
            eliminate(i);
        }
    }

    List<AffineFraction> getList(String name, boolean isLower) {
        if (isLower) {
            if (lowerBound.containsKey(name)) {
                return lowerBound.get(name);
            } else {
                List<AffineFraction> list = new ArrayList<>();
                lowerBound.put(name, list);
                return list;
            }
        } else {
            if (upperBound.containsKey(name)) {
                return upperBound.get(name);
            } else {
                List<AffineFraction> list = new ArrayList<>();
                upperBound.put(name, list);
                return list;
            }
        }
    }


    void eliminate(int num) {
        var listLower = getList("f" + num, true);
        var listUpper = getList("f" + num, false);
        for (AffineFraction lower : listLower) {
            for (AffineFraction upper : listUpper) {
                for (int k = num; k >= -1; --k) {
                    if (k == -1) {
                        if (upper.isConst() && lower.isConst() && upper.bias.less(lower.bias)) {
                            throw new RuntimeException("No solution!");
                        }
                        // TODO: the check of program-para
                        break;
                    }
                    if (lower.getCoe("f" + k).less(upper.getCoe("f" + k))) {
                        var coe = lower.getCoe("f" + k).sub(upper.getCoe("f" + k));
                        AffineFraction affine = new AffineFraction(upper).merge(lower, new Fraction(-1)).div(coe);
                        affine.remove("f" + k);
                        getList("f" + k, true).add(affine);
                        break;
                    } else if (upper.getCoe("f" + k).less(lower.getCoe("f" + k))) {
                        var coe = lower.getCoe("f" + k).sub(upper.getCoe("f" + k));
                        AffineFraction affine = new AffineFraction(upper).merge(lower, new Fraction(-1)).div(coe);
                        affine.remove("f" + k);
                        getList("f" + k, false).add(affine);
                        break;
                    }
                }
            }
        }
    }
}
