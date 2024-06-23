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
import java.util.List;

import static src.polyhedral.dependency.Constrain.*;

public class Schedule {

    public Model model;
    private IloCplex cplex;
    private HashMap<Coordinates, List<IloIntVar>> coefficient;
    private IloIntVar costW;
    private HashMap<Coordinates, List<List<Integer>>> coefficientAns;
    public HashMap<Coordinates, Matrix> transforms;
    public HashMap<Coordinates, Matrix> transformInverses;
    public HashMap<Coordinates, Matrix> hermites;
    public FourierMotzkin fourierMotzkin;

    public Schedule(Model model_) {
        model = model_;
        coefficient = new HashMap<>();
        coefficientAns = new HashMap<>();
        transforms = new HashMap<>();
        transformInverses = new HashMap<>();
        hermites = new HashMap<>();
        try {
            cplex = new IloCplex();
            costW = cplex.intVar(0, Integer.MAX_VALUE);
            cplex.addGe(costW, 1);
            int dim = 0;
            // set undetermined coefficient
            for (var assign : model.domain.stmtList) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                List<IloIntVar> tmp = new ArrayList<>();
                dim = assign.coordinates.dim;
                for (int i = 0; i < assign.coordinates.dim; ++i) { // the last term is bias coefficient
                    IloIntVar x = cplex.intVar(0, Integer.MAX_VALUE);
                    tmp.add(x);
                    expr.addTerm(1, x);
                }
                IloIntVar x = cplex.intVar(0, Integer.MAX_VALUE);
                tmp.add(x); // bias
                coefficient.put(assign.coordinates, tmp);
                coefficientAns.put(assign.coordinates, new ArrayList<>());
                cplex.addGe(expr, 1); // avoid zero-vec
            }
            // set dependency and cost bound
            for (Dependency dependency : model.dependencies) {
                for (int i = 0; i < dependency.lexicographic.constrains.size(); ++i) {
                    setDependency(dependency, i);
                    setCostBound(dependency, i);
                }
            }
            // set target (min-lexi of cost function)
            IloLinearNumExpr target = cplex.linearNumExpr();
            target.addTerm(100000, costW);
            for (var list : coefficient.values()) {
                for (var iloIntVar : list) {
                    target.addTerm(1, iloIntVar);
                }
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
    }

    public boolean solve() {
        try {
            boolean isSolve = cplex.solve();
            if (isSolve) {
                //如果找到了解
                double result = cplex.getObjValue();  // 获取解（目标函数最大值）
                System.out.println("目标函数最大值为：" + result);
                double x_value = cplex.getValue(costW);
                System.err.println("c = " + x_value);
                for (var list : coefficient.values()) {
                    for (var iloIntVar : list) {
                        x_value = cplex.getValue(iloIntVar);
                        System.err.println("c = " + x_value);
                    }
                }
                System.err.println("成功！");
                for (var entry : coefficient.entrySet()) {
                    List<Integer> tmp = new ArrayList<>();
                    for (var x : entry.getValue()) {
                        tmp.add((int) cplex.getValue(x));
                    }
                    coefficientAns.get(entry.getKey()).add(tmp);
                }
                for (Coordinates coordinates : coefficient.keySet()) {
                    setOrthogonal(coordinates);
                }
            } else {
                // 如果找不到解
                System.err.println("失败！");
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public void reorder() {
        for (Coordinates coordinates : coefficientAns.keySet()) {
            transform(coordinates);
        }
        fourierMotzkin = new FourierMotzkin(model.domain.indexList);
        for (Matrix transformInverse : transformInverses.values()) {
            fourierMotzkin.setTransform(transformInverse);
        }
        print();
    }

    void setDependency(Dependency dependency, int lexiDim) throws IloException {
        // use Farkas Lemma
        HashMap<String, IloLinearNumExpr> lhs = new HashMap<>(); // "$" represents constant
        HashMap<String, IloLinearNumExpr> rhs = new HashMap<>(); // "$" represents constant
        for (int i = 0; i < dependency.coordinatesTo.dim; ++i) {
            getExp(dependency.coordinatesTo.varName.get(i) + "#" + dependency.id, lhs)
                    .addTerm(1, coefficient.get(dependency.coordinatesTo).get(i));
        }
        getExp("$", lhs).addTerm(1, coefficient.get(dependency.coordinatesTo).get(dependency.coordinatesTo.dim));
        for (int i = 0; i < dependency.coordinatesFrom.dim; ++i) {
            getExp(dependency.coordinatesFrom.varName.get(i), lhs)
                    .addTerm(-1, coefficient.get(dependency.coordinatesFrom).get(i));
        }
        getExp("$", lhs).addTerm(-1, coefficient.get(dependency.coordinatesFrom).get(dependency.coordinatesFrom.dim));
        for (Constrain constrain : dependency.constrains) {
            setFarkas(constrain, rhs);
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            setFarkas(constrain, rhs);
        }
        for (String varName : lhs.keySet()) {
            if (!varName.equals("$")) {
                Index index = dependency.indexBound.get(getName(varName));
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                getExp("$", rhs).addTerm(-index.boundFrom, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                getExp("$", rhs).addTerm(index.boundTo, farkas2);
            }
        }
        for (String varName : rhs.keySet()) {
            if (!lhs.containsKey(varName) && !varName.equals("$")) {
                Index index = dependency.indexBound.get(getName(varName));
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                getExp("$", rhs).addTerm(-index.boundFrom, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                getExp("$", rhs).addTerm(index.boundTo, farkas2);
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
        getExp("$", lhs).addTerm(1, costW);
        for (int i = 0; i < dependency.coordinatesTo.dim; ++i) {
            getExp(dependency.coordinatesTo.varName.get(i) + "#" + dependency.id, lhs)
                    .addTerm(-1, coefficient.get(dependency.coordinatesTo).get(i));
        }
        getExp("$", lhs).addTerm(-1, coefficient.get(dependency.coordinatesTo).get(dependency.coordinatesTo.dim));
        for (int i = 0; i < dependency.coordinatesFrom.dim; ++i) {
            getExp(dependency.coordinatesFrom.varName.get(i), lhs)
                    .addTerm(1, coefficient.get(dependency.coordinatesFrom).get(i));
        }
        getExp("$", lhs).addTerm(1, coefficient.get(dependency.coordinatesFrom).get(dependency.coordinatesFrom.dim));
        for (Constrain constrain : dependency.constrains) {
            setFarkas(constrain, rhs);
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            setFarkas(constrain, rhs);
        }
        for (String varName : lhs.keySet()) {
            if (!varName.equals("$")) {
                Index index = dependency.indexBound.get(getName(varName));
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                getExp("$", rhs).addTerm(-index.boundFrom, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                getExp("$", rhs).addTerm(index.boundTo, farkas2);
            }
        }
        for (String varName : rhs.keySet()) {
            if (!lhs.containsKey(varName) && !varName.equals("$")) {
                Index index = dependency.indexBound.get(getName(varName));
                IloIntVar farkas1 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(1, farkas1);
                getExp("$", rhs).addTerm(-index.boundFrom, farkas1);
                IloIntVar farkas2 = cplex.intVar(0, Integer.MAX_VALUE);
                getExp(varName, rhs).addTerm(-1, farkas2);
                getExp("$", rhs).addTerm(index.boundTo, farkas2);
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

    Matrix orthogonalSpace(Coordinates coordinates) {
        int col = coefficient.get(coordinates).size() - 1;
        int row = coefficientAns.get(coordinates).size();
        Matrix Hs = new Matrix(row, col);
        for (int i = 0; i < row; ++i) {
            var tmp = coefficientAns.get(coordinates).get(i);
            for (int j = 0; j < col; ++j) {
                Hs.setElement(i, j, new Fraction(tmp.get(j)));
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

    void setOrthogonal(Coordinates coordinates) throws IloException {
        Matrix Hs = orthogonalSpace(coordinates);
        var valid = Hs.findDependent();
        var expTol = cplex.linearNumExpr();
        var list = coefficient.get(coordinates);
        for (int i : valid) {
            var exp = cplex.linearNumExpr();
            for (int j = 0; j < list.size() - 1; ++j) {
                exp.addTerm(Hs.getElement(i, j).toInt(), list.get(j));
            }
            cplex.addGe(exp, 0);
            expTol.add(exp);
        }
        cplex.addGe(expTol, 2); // avoid zero-vec
    }

    String getName(String varName) {
        int hashIndex = varName.indexOf('#');
        if (hashIndex != -1) {
            return varName.substring(0, hashIndex);
        } else {
            return varName;
        }
    }

    public void transform(Coordinates coordinates) {
        int row = coefficient.get(coordinates).size() - 1;
        int col = coefficientAns.get(coordinates).size();
        Matrix coefficientMatrix = new Matrix(row, col);
        for (int i = 0; i < row; ++i) {
            var list = coefficientAns.get(coordinates).get(i);
            for (int j = 0; j < col; ++j) {
                coefficientMatrix.setElement(i, j, new Fraction(list.get(j)));
            }
        }
        transforms.put(coordinates, coefficientMatrix);
        transformInverses.put(coordinates, coefficientMatrix.inverse());
        hermites.put(coordinates, coefficientMatrix.getHermite());
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
                for (var matrix : hermites.values()) {
                    System.out.print(matrix.getElement(i, i));
                }
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
            for (var matrix : hermites.values()) {
                flag = false;
                for (int j = 0; j < num; ++j) {
                    if (flag && !matrix.getElement(num, j).less(0)) {
                        System.out.print("+");
                    }
                    flag = true;
                    System.out.print(matrix.getElement(num, j));
                    System.out.print("*f" + j);
                }
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
            for (var matrix : hermites.values()) {
                System.out.print(matrix.getElement(num, num));
            }
            System.out.print(", ");
        }
    }

    public void printUpperBound(int num) {
        var list = fourierMotzkin.upperBound.get("f" + num);
        for (int i = 0; i < list.size(); ++i) {
            boolean flag = true;
            var affine = list.get(i);
            for (var entry : affine.coefficient.entrySet()) {
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
            if (affine.bias.less(0)) {
                System.out.print(affine.bias);
            } else {
                System.out.print("+" + affine.bias);
            }
            if (!affine.coefficient.isEmpty()) {
                System.out.print(")");
            }
            System.out.print(", ");
/////////////////////////////////////////////////
        }
    }

    public void printTransfer(int num) {
        System.out.print("\t\t" + model.domain.indexList.get(num).varName + " -> ");
        for (var matrix : transformInverses.values()) {
            boolean flag = false;
            for (int i = 0; i < matrix.row(); ++i) {
                if (flag && !matrix.getElement(num, i).less(0)) {
                    System.out.print(" + ");
                } else {
                    System.out.print(" ");
                }
                flag = true;
                System.out.print(matrix.getElement(num, i));
                System.out.print("*f" + i);
            }
        }
        System.out.print("\n");
    }
}
