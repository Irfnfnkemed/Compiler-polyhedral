package src.polyhedral.schedule;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import src.polyhedral.dependency.Constrain;
import src.polyhedral.dependency.Dependency;
import src.polyhedral.dependency.Model;
import src.polyhedral.extract.Coordinates;
import src.polyhedral.extract.Index;
import src.polyhedral.matrix.Fraction;
import src.polyhedral.matrix.Matrix;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static src.polyhedral.dependency.Constrain.*;

public class Schedule {

    public Model model;
    private IloCplex cplex;
    private List<IloIntVar> coefficient;
    private HashMap<Coordinates, IloIntVar> bias;
    private HashMap<String, IloIntVar> cost; // "$" represents the bias
    public HashMap<Coordinates, List<Integer>> transformationBias;
    public Matrix transformation;
    public Matrix transformInverse;
    public Matrix hermite;
    public FourierMotzkin fourierMotzkin;
    private int nowRow = 0;

    public Schedule(Model model_) {
        int dim = model_.indexBound.size();
        model = model_;
        coefficient = new ArrayList<>();
        transformation = new Matrix(dim, dim);
        bias = new HashMap<>();
        cost = new HashMap<>();
        transformationBias = new HashMap<>();
        try {
            cplex = new IloCplex();
            // set undetermined coefficient
            IloLinearNumExpr expr = cplex.linearNumExpr();
            for (int i = 0; i < dim; ++i) {
                IloIntVar x = cplex.intVar(0, Integer.MAX_VALUE);
                coefficient.add(x);
                expr.addTerm(1, x);
            }
            cplex.addGe(expr, 1); // avoid zero-vec
            for (var assign : model.domain.stmtList) {
                bias.put(assign.coordinates, cplex.intVar(0, Integer.MAX_VALUE)); // bias
            }
            if (!model.domain.parameters.isEmpty()) {
                for (String para : model.domain.parameters) {
                    cost.put(para, cplex.intVar(0, Integer.MAX_VALUE));
                }
            }
            cost.put("$", cplex.intVar(0, Integer.MAX_VALUE));
            // set dependency and cost bound
            for (Dependency dependency : model.dependencies) {
                for (int i = 0; i < dependency.lexicographic.constrains.size(); ++i) {
                    setDependency(dependency, i);
                    setCostBound(dependency, i);
                }
            }
            // set target (min-lexi of cost function)
            IloLinearNumExpr target = cplex.linearNumExpr();
            for (var entry : cost.entrySet()) {
                if (!entry.getKey().equals("$")) {
                    target.addTerm(1000000, entry.getValue());
                }
            }
            target.addTerm(1000, cost.get("$"));
            for (var iloIntVar : coefficient) {
                target.addTerm(1, iloIntVar);
            }
            cplex.addMinimize(target);
            for (int i = 0; i < dim; ++i) {
                if (!solve()) {
                    return;
                }
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        reorder();
        System.out.println(transformation);
    }

    public boolean solve() {
        try {
            boolean isSolve = cplex.solve();
            if (isSolve) {
                ///////////////////////////////////////debug
                //如果找到了解
                double result = cplex.getObjValue();  // 获取解（目标函数最大值）
                System.out.println("目标函数最大值为：" + result);
                double x_value = cplex.getValue(cost.get("$"));
                System.err.println("w = " + x_value);
                for (var iloIntVar : cost.values()) {
                    x_value = cplex.getValue(iloIntVar);
                    System.err.println("s = " + x_value);
                }
                for (var iloIntVar : coefficient) {
                    x_value = cplex.getValue(iloIntVar);
                    System.err.println("c = " + x_value);
                }
                for (var entry : bias.entrySet()) {
                    x_value = cplex.getValue(entry.getValue());
                    System.err.println("b = " + x_value);
                }
                System.err.println("成功！");
                //////////////////////////////////////////////
                for (int i = 0; i < coefficient.size(); ++i) {
                    int value = (int) Math.round(cplex.getValue(coefficient.get(i)));
                    transformation.setElement(nowRow, i, new Fraction(value));
                }
                for (var entry : bias.entrySet()) {
                    if (transformationBias.containsKey(entry.getKey())) {
                        transformationBias.get(entry.getKey()).add((int) Math.round(cplex.getValue(entry.getValue())));
                    } else {
                        List<Integer> tmp = new ArrayList<>();
                        tmp.add((int) Math.round(cplex.getValue(entry.getValue())));
                        transformationBias.put(entry.getKey(), tmp);
                    }
                }
                ++nowRow;
                setOrthogonal();
                return true;
            } else {
                // 如果找不到解
                System.err.println("失败！");
                return false;
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    public void reorder() {
        transformInverse = transformation.inverse();
        hermite = transformation.getHermite();
        fourierMotzkin = new FourierMotzkin(model.domain.indexList);
        fourierMotzkin.setTransform(transformInverse);
        print();
    }

    void setDependency(Dependency dependency, int lexiDim) throws IloException {
        // use Farkas Lemma
        HashMap<String, IloLinearNumExpr> lhs = new HashMap<>(); // "$" represents constant
        HashMap<String, IloLinearNumExpr> rhs = new HashMap<>(); // "$" represents constant
        for (int i = 0; i < dependency.coordinatesTo.dim; ++i) {
            getExp(dependency.coordinatesTo.varName.get(i) + "#" + dependency.id, lhs).addTerm(1, coefficient.get(i));
        }
        getExp("$", lhs).addTerm(1, bias.get(dependency.coordinatesTo));
        for (int i = 0; i < dependency.coordinatesFrom.dim; ++i) {
            getExp(dependency.coordinatesFrom.varName.get(i), lhs).addTerm(-1, coefficient.get(i));
        }
        getExp("$", lhs).addTerm(-1, bias.get(dependency.coordinatesFrom));
        for (Constrain constrain : dependency.constrains) {
            setFarkas(constrain, rhs);
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            setFarkas(constrain, rhs);
        }
        HashSet<String> varNames = new HashSet<>(lhs.keySet());
        varNames.addAll(rhs.keySet());
        varNames.remove("$");
        for (String varName : varNames) {
            Index index = dependency.indexBound.get(getName(varName));
            if (index != null) {
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                if (!index.boundFrom.isConst()) {
                    for (var entry : index.boundFrom.coefficient.entrySet()) {
                        getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas1);
                    }
                }
                getExp("$", rhs).addTerm(-index.boundFrom.bias, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                if (!index.boundTo.isConst()) {
                    for (var entry : index.boundTo.coefficient.entrySet()) {
                        getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas2);
                    }
                }
                getExp("$", rhs).addTerm(index.boundTo.bias, farkas2);
            }
        }
        IloIntVar farkas = cplex.intVar(0, Integer.MAX_VALUE); // Farkas variable
        getExp("$", rhs).addTerm(1, farkas);
        for (var entry : lhs.entrySet()) {
            if (rhs.containsKey(entry.getKey())) {
                cplex.addEq(entry.getValue(), rhs.get(entry.getKey()));
            } else {
                cplex.addEq(entry.getValue(), 0);
            }
        }
        for (var entry : rhs.entrySet()) {
            if (!lhs.containsKey(entry.getKey())) {
                cplex.addEq(entry.getValue(), 0);
            }
        }
    }

    void setCostBound(Dependency dependency, int lexiDim) throws IloException {
        // use Farkas Lemma
        HashMap<String, IloLinearNumExpr> lhs = new HashMap<>(); // "$" represents constant
        HashMap<String, IloLinearNumExpr> rhs = new HashMap<>(); // "$" represents constant
        for (var entry : cost.entrySet()) {
            getExp(entry.getKey(), lhs).addTerm(1, entry.getValue());
        }
        for (int i = 0; i < dependency.coordinatesTo.dim; ++i) {
            getExp(dependency.coordinatesTo.varName.get(i) + "#" + dependency.id, lhs).addTerm(-1, coefficient.get(i));
        }
        getExp("$", lhs).addTerm(-1, bias.get(dependency.coordinatesTo));
        for (int i = 0; i < dependency.coordinatesFrom.dim; ++i) {
            getExp(dependency.coordinatesFrom.varName.get(i), lhs).addTerm(1, coefficient.get(i));
        }
        getExp("$", lhs).addTerm(1, bias.get(dependency.coordinatesFrom));
        for (Constrain constrain : dependency.constrains) {
            setFarkas(constrain, rhs);
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            setFarkas(constrain, rhs);
        }
        HashSet<String> varNames = new HashSet<>(lhs.keySet());
        varNames.addAll(rhs.keySet());
        varNames.remove("$");
        for (String varName : varNames) {
            Index index = dependency.indexBound.get(getName(varName));
            if (index != null) {
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                if (!index.boundFrom.isConst()) {
                    for (var entry : index.boundFrom.coefficient.entrySet()) {
                        getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas1);
                    }
                }
                getExp("$", rhs).addTerm(-index.boundFrom.bias, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                if (!index.boundTo.isConst()) {
                    for (var entry : index.boundTo.coefficient.entrySet()) {
                        getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas2);
                    }
                }
                getExp("$", rhs).addTerm(index.boundTo.bias, farkas2);
            }
        }
        IloIntVar farkas = cplex.intVar(0, Integer.MAX_VALUE); // Farkas variable
        getExp("$", rhs).addTerm(1, farkas);
        for (var entry : lhs.entrySet()) {
            if (rhs.containsKey(entry.getKey())) {
                cplex.addEq(entry.getValue(), rhs.get(entry.getKey()));
            } else {
                cplex.addEq(entry.getValue(), 0);
            }
        }
        for (var entry : rhs.entrySet()) {
            if (!lhs.containsKey(entry.getKey())) {
                cplex.addEq(entry.getValue(), 0);
            }
        }
    }

    void setFarkas(Constrain constrain, HashMap<String, IloLinearNumExpr> collect) throws IloException {
        if (constrain.op == LE || constrain.op == EQ) {
            IloIntVar farkas = cplex.intVar(0, Integer.MAX_VALUE); // Farkas variable
            for (var entry : constrain.rhs.coefficient.entrySet()) {
                getExp(entry.getKey(), collect).addTerm(entry.getValue(), farkas);
            }
            for (var entry : constrain.lhs.coefficient.entrySet()) {
                getExp(entry.getKey(), collect).addTerm(-entry.getValue(), farkas);
            }
            if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                getExp("$", collect).addTerm(constrain.rhs.bias - constrain.lhs.bias, farkas);
            }
        }
        if (constrain.op == GE || constrain.op == EQ) {
            IloIntVar farkas = cplex.intVar(0, Integer.MAX_VALUE); // Farkas variable
            for (var entry : constrain.lhs.coefficient.entrySet()) {
                getExp(entry.getKey(), collect).addTerm(entry.getValue(), farkas);
            }
            for (var entry : constrain.rhs.coefficient.entrySet()) {
                getExp(entry.getKey(), collect).addTerm(-entry.getValue(), farkas);
            }
            if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                getExp("$", collect).addTerm(constrain.lhs.bias - constrain.rhs.bias, farkas);
            }
        }
    }

    IloLinearNumExpr getExp(String varName, HashMap<String, IloLinearNumExpr> map) throws IloException {
        if (!map.containsKey(varName)) {
            map.put(varName, cplex.linearNumExpr());
        }
        return map.get(varName);
    }

    Matrix orthogonalSpace() {
        int col = model.indexBound.size();
        Matrix Hs = new Matrix(nowRow, col);
        for (int i = 0; i < nowRow; ++i) {
            for (int j = 0; j < col; ++j) {
                Hs.setElement(i, j, new Fraction(transformation.getElement(i, j)));
            }
        }
        Matrix identity = new Matrix(col, col);
        for (int i = 0; i < col; ++i) {
            for (int j = 0; j < col; ++j) {
                if (i == j) {
                    identity.setElement(i, j, new Fraction(1));
                } else {
                    identity.setElement(i, j, new Fraction(0));
                }
            }
        }
        Matrix Hst = Hs.trans();
        Matrix inv = (Hs.mul(Hst)).inverse();
        Fraction det = (Hs.mul(Hst)).det();
        return (identity.sub((Hst.mul(inv).mul(Hs)))).scalarMul(det);
    }

    void setOrthogonal() throws IloException {
        Matrix Hs = orthogonalSpace();
        var valid = Hs.findDependent();
        var expTol = cplex.linearNumExpr();
        for (int i : valid) {
            var exp = cplex.linearNumExpr();
            for (int j = 0; j < coefficient.size(); ++j) {
                exp.addTerm(Hs.getElement(i, j).toInt(), coefficient.get(j));
            }
            cplex.addGe(exp, 0);
            expTol.add(exp);
        }
        cplex.addGe(expTol, 1); // avoid zero-vec
    }

    String getName(String varName) {
        int hashIndex = varName.indexOf('#');
        if (hashIndex != -1) {
            return varName.substring(0, hashIndex);
        } else {
            return varName;
        }
    }

    public void print() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("./src/builtin/transfer");
            PrintStream printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            System.out.println("loop transfer:");
            for (int i = 0; i < fourierMotzkin.indexList.size(); ++i) {
                System.out.print("new dim" + i + ": f" + i);
                System.out.print(";   lower bound: MAX{ ");
                printLowerBound(i);
                System.out.print("};   upper bound: MIN{ ");
                printUpperBound(i);
                System.out.print("};   step: ");
                System.out.print(hermite.getElement(i, i));
                System.out.print("\n");
            }
            for (int i = 0; i < fourierMotzkin.indexList.size(); ++i) {
                printTransfer(i);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void printLowerBound(int num) {
        var list = fourierMotzkin.lowerBound.get("f" + num);
        for (int i = 0; i < list.size(); ++i) {
            boolean flag = true;
            var affine = list.get(i);
            for (var entry : affine.coefficient.entrySet()) {
                if (flag) {
                    System.out.print("cell(");
                    flag = false;
                } else if (!entry.getValue().less(0) && !entry.getValue().equal(0)) {
                    System.out.print("+");
                }
                System.out.print(entry.getValue());
                System.out.print("*");
                System.out.print(entry.getKey());
            }
            if (affine.bias.less(0) || (affine.bias.equal(0) && flag)) {
                System.out.print(affine.bias);
            } else {
                System.out.print("+" + affine.bias);
            }
            if (!affine.coefficient.isEmpty()) {
                System.out.print(")");
            }
            System.out.print("+(");
            flag = false;
            for (int j = 0; j < num; ++j) {
                if (flag && !hermite.getElement(num, j).less(0)) {
                    System.out.print("+");
                }
                flag = true;
                System.out.print(hermite.getElement(num, j));
                System.out.print("*f" + j);
            }
            System.out.print("-");
            flag = true;
            for (var entry : affine.coefficient.entrySet()) {
                if (flag) {
                    System.out.print("cell(");
                    flag = false;
                } else if (!entry.getValue().less(0) && !entry.getValue().equal(0)) {
                    System.out.print("+");
                }
                System.out.print(entry.getValue());
                System.out.print("*");
                System.out.print(entry.getKey());
            }
            if (affine.bias.less(0) || (affine.bias.equal(0) && flag)) {
                System.out.print(affine.bias);
            } else {
                System.out.print("+" + affine.bias);
            }
            if (!affine.coefficient.isEmpty()) {
                System.out.print(")");
            }
            System.out.print(")%");
            System.out.print(hermite.getElement(num, num));
            System.out.print(", ");
        }
    }

    public void printUpperBound(int num) {
        var list = fourierMotzkin.upperBound.get("f" + num);
        for (AffineFraction affineFraction : list) {
            boolean flag = true;
            for (var entry : affineFraction.coefficient.entrySet()) {
                if (flag) {
                    System.out.print("floor(");
                    flag = false;
                } else if (!entry.getValue().less(0) && !entry.getValue().equal(0)) {
                    System.out.print("+");
                }
                System.out.print(entry.getValue());
                System.out.print("*");
                System.out.print(entry.getKey());
            }
            if (affineFraction.bias.less(0)) {
                System.out.print(affineFraction.bias);
            } else {
                System.out.print("+" + affineFraction.bias);
            }
            if (!affineFraction.coefficient.isEmpty()) {
                System.out.print(")");
            }
            System.out.print(", ");
/////////////////////////////////////////////////
        }
    }

    public void printTransfer(int num) {
        System.out.print("\t\t" + model.domain.indexList.get(num).varName + " -> ");
        boolean flag = false;
        for (int i = 0; i < transformInverse.row(); ++i) {
            if (flag && !transformInverse.getElement(num, i).less(0)) {
                System.out.print(" + ");
            } else {
                System.out.print(" ");
            }
            flag = true;
            System.out.print(transformInverse.getElement(num, i));
            System.out.print("*f" + i);
        }
        System.out.print("\n");
    }
}
