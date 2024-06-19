package src.polyhedral.dependency;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import src.polyhedral.extract.Affine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.max;
import static src.polyhedral.dependency.Constrain.*;

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
            if (checkNonEmpty(constrainTmp)) {
                constrains.add(constrainTmp);
            }
            if (dependency.coordinatesFrom.stmtId.get(i) > dependency.coordinatesTo.stmtId.get(i)) {
                break;
            }
            if (dependency.coordinatesFrom.stmtId.get(i) < dependency.coordinatesTo.stmtId.get(i)) {
                List<Constrain> constrainTmp2 = new ArrayList<>();
                for (int j = 0; j <= i; ++j) {
                    pushEqual(constrainTmp2, j);
                }
                if (checkNonEmpty(constrainTmp2)) {
                    constrains.add(constrainTmp2);
                }
                break;
            }
        }
    }

    void pushLess(List<Constrain> list, int dim) {
        Affine lhs = new Affine().addVarCo(dependency.coordinatesFrom.varName.get(dim), 1).addBias(1);
        Affine rhs = new Affine().addVarCo(dependency.coordinatesTo.varName.get(dim) + "#" + dependency.id, 1);
        Constrain constrain = new Constrain(lhs, rhs, LE);
        list.add(constrain);
    }

    void pushEqual(List<Constrain> list, int dim) {
        Affine lhs = new Affine().addVarCo(dependency.coordinatesFrom.varName.get(dim), 1);
        Affine rhs = new Affine().addVarCo(dependency.coordinatesTo.varName.get(dim) + "#" + dependency.id, 1);
        Constrain constrain = new Constrain(lhs, rhs, EQ);
        list.add(constrain);
    }

    String getName(String varName) {
        int hashIndex = varName.indexOf('#');
        if (hashIndex != -1) {
            return varName.substring(0, hashIndex);
        } else {
            return varName;
        }
    }

    void setVar(Constrain constrain, HashMap<String, IloNumVar> varMap, IloCplex cplex) throws IloException {
        if (!constrain.lhs.coefficient.isEmpty()) {
            for (String varName : constrain.lhs.coefficient.keySet()) {
                if (!varMap.containsKey(varName)) {
                    var index = dependency.indexBound.get(getName(varName));
                    IloNumVar x = cplex.intVar((int) index.boundFrom, (int) index.boundTo); // TODO: normalize the index bound
                    varMap.put(varName, x);
                }
            }
        }
        if (!constrain.rhs.coefficient.isEmpty()) {
            for (String varName : constrain.rhs.coefficient.keySet()) {
                if (!varMap.containsKey(varName)) {
                    var index = dependency.indexBound.get(getName(varName));
                    IloNumVar x = cplex.intVar((int) index.boundFrom, (int) index.boundTo); // TODO: normalize the index bound
                    varMap.put(varName, x);
                }
            }
        }
    }

    void setConstrain(Constrain constrain, HashMap<String, IloNumVar> varMap, IloCplex cplex) throws IloException {
        IloLinearNumExpr expr = cplex.linearNumExpr();
        if (!constrain.lhs.coefficient.isEmpty()) {
            for (var entry : constrain.lhs.coefficient.entrySet()) {
                expr.addTerm(varMap.get(entry.getKey()), entry.getValue());
            }
        }
        if (!constrain.rhs.coefficient.isEmpty()) {
            for (var entry : constrain.rhs.coefficient.entrySet()) {
                expr.addTerm(varMap.get(entry.getKey()), -entry.getValue());
            }
        }
        if (constrain.op == EQ) {
            cplex.addEq(expr, constrain.rhs.bias - constrain.lhs.bias);
        } else if (constrain.op == LE) {
            cplex.addLe(expr, constrain.rhs.bias - constrain.lhs.bias);
        } else if (constrain.op == GE) {
            cplex.addGe(expr, constrain.rhs.bias - constrain.lhs.bias);
        }
    }


    boolean checkNonEmpty(List<Constrain> list) {
        HashMap<String, IloNumVar> varMap = new HashMap<>();
        try {
            IloCplex cplex = new IloCplex();
            // set variable
            for (Constrain constrain : dependency.constrains) {
                setVar(constrain, varMap, cplex);
            }
            for (Constrain constrain : list) {
                setVar(constrain, varMap, cplex);
            }
            // add constrains
            for (Constrain constrain : dependency.constrains) {
                setConstrain(constrain, varMap, cplex);
            }
            for (Constrain constrain : list) {
                setConstrain(constrain, varMap, cplex);
            }
            boolean isSolve = cplex.solve();
            if (isSolve) {
                System.err.println("合法");
            } else {
                System.err.println("不合法");
            }
            return isSolve;
        } catch (IloException e) {
            System.err.println("???");
            return false;
        }
    }

    public boolean valid() {
        return !constrains.isEmpty();
    }
}
