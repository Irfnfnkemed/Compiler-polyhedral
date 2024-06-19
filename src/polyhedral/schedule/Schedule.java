package src.polyhedral.schedule;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import src.polyhedral.dependency.Constrain;
import src.polyhedral.dependency.Dependency;
import src.polyhedral.dependency.Model;
import src.polyhedral.extract.Affine;
import src.polyhedral.extract.Coordinates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static src.polyhedral.dependency.Constrain.*;

public class Schedule {

    public Model model;
    public IloCplex cplex;
    public HashMap<Coordinates, List<IloNumVar>> coefficient;
    public IloNumVar costW;

    public Schedule(Model model_) {
        model = model_;
        try {
            cplex = new IloCplex();
            coefficient = new HashMap<>();
            costW = cplex.intVar(0, Integer.MAX_VALUE);
            cplex.addGe(costW, 1);
            // set undetermined coefficient
            for (var assign : model.domain.stmtList) {
                IloLinearNumExpr expr = cplex.linearNumExpr();
                List<IloNumVar> tmp = new ArrayList<>();
                for (int i = 0; i < assign.coordinates.dim; ++i) { // the last term is bias coefficient
                    IloNumVar x = cplex.intVar(0, Integer.MAX_VALUE);
                    tmp.add(x);
                    expr.addTerm(1, x);
                }
                IloNumVar x = cplex.intVar(0, Integer.MAX_VALUE);
                tmp.add(x); // bias
                coefficient.put(assign.coordinates, tmp);
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
                for (var iloNumVar : list) {
                    target.addTerm(1, iloNumVar);
                }
            }
            cplex.addMinimize(target);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
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
                    for (var iloNumVar : list) {
                        x_value = cplex.getValue(iloNumVar);
                        System.err.println("c = " + x_value);
                    }
                }
                System.err.println("成功！");
            } else {
                // 如果找不到解
                System.err.println("失败！");
            }
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
        return false;
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
            if (constrain.op == LE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.rhs.bias - constrain.lhs.bias, farkas);
                }
            }
            if (constrain.op == GE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.lhs.bias - constrain.rhs.bias, farkas);
                }
            }
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            if (constrain.op == LE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.rhs.bias - constrain.lhs.bias, farkas);
                }
            }
            if (constrain.op == GE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.lhs.bias - constrain.rhs.bias, farkas);
                }
            }
        }
        IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
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
            if (constrain.op == LE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.rhs.bias - constrain.lhs.bias, farkas);
                }
            }
            if (constrain.op == GE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.lhs.bias - constrain.rhs.bias, farkas);
                }
            }
        }
        for (Constrain constrain : dependency.lexicographic.constrains.get(lexiDim)) {
            if (constrain.op == LE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.rhs.bias - constrain.lhs.bias, farkas);
                }
            }
            if (constrain.op == GE || constrain.op == EQ) {
                IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
                for (var entry : constrain.lhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(entry.getValue(), farkas);
                }
                for (var entry : constrain.rhs.coefficient.entrySet()) {
                    getExp(entry.getKey(), rhs).addTerm(-entry.getValue(), farkas);
                }
                if (constrain.lhs.bias - constrain.rhs.bias != 0) {
                    getExp("$", rhs).addTerm(constrain.lhs.bias - constrain.rhs.bias, farkas);
                }
            }
        }
        IloNumVar farkas = cplex.numVar(0, Integer.MAX_VALUE); // Farkas variable
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

    IloLinearNumExpr getExp(String varName, HashMap<String, IloLinearNumExpr> map) throws IloException {
        if (!map.containsKey(varName)) {
            map.put(varName, cplex.linearNumExpr());
        }
        return map.get(varName);
    }
}
