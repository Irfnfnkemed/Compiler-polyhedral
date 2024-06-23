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

    void setTransform(Matrix transformInverse){
        for (int i = 0; i < transformInverse.row(); ++i) { // set initial constrains
            for (int j = transformInverse.row() - 1; j >= 0; --j) {
                var coe = transformInverse.getElement(i, j);
                if (coe.equal(0)) {
                    continue;
                } else if (coe.less(0)) {
                    AffineFraction affine1 = new AffineFraction();
                    affine1.addBias(new Fraction((int) indexList.get(i).boundTo).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine1.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, true).add(affine1);
                    AffineFraction affine2 = new AffineFraction();
                    affine2.addBias(new Fraction((int) indexList.get(i).boundFrom).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine2.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, false).add(affine2);
                    break;
                } else {
                    AffineFraction affine1 = new AffineFraction();
                    affine1.addBias(new Fraction((int) indexList.get(i).boundFrom).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine1.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, true).add(affine1);
                    AffineFraction affine2 = new AffineFraction();
                    affine2.addBias(new Fraction((int) indexList.get(i).boundTo).div(coe));
                    for (int k = j - 1; k >= 0; --k) {
                        affine2.addVarCo("f" + k, transformInverse.getElement(i, k).neg().div(coe));
                    }
                    getList("f" + j, false).add(affine2);
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
                        if (upper.bias.less(lower.bias)) {
                            throw new RuntimeException("No solution!");
                        }
                        break;
                    }
                    if (lower.getCoe("f" + k).less(upper.getCoe("f" + k))) {
                        var coe = upper.getCoe("f" + k).sub(lower.getCoe("f" + k));
                        AffineFraction affine = new AffineFraction();
                        affine.addBias(new Fraction((lower.bias.sub(upper.bias)).div(coe)));
                        for (int t = k - 1; t >= 0; --t) {
                            affine.addVarCo("f" + t, new Fraction((lower.getCoe("f" + t).sub(upper.getCoe("f" + t))).div(coe)));
                        }
                        getList("f" + k, true).add(affine);
                        break;
                    } else if (upper.getCoe("f" + k).less(lower.getCoe("f" + k))) {
                        var coe = lower.getCoe("f" + k).sub(upper.getCoe("f" + k));
                        AffineFraction affine = new AffineFraction();
                        affine.addBias(new Fraction((upper.bias.sub(lower.bias)).div(coe)));
                        for (int t = k - 1; t >= 0; --t) {
                            affine.addVarCo("f" + t, new Fraction((upper.getCoe("f" + t).sub(lower.getCoe("f" + t))).div(coe)));
                        }
                        getList("f" + k, false).add(affine);
                        break;
                    }
                }
            }
        }
    }
}
