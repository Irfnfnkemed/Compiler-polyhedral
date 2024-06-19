package src.polyhedral.extract;

import src.AST.definition.variableDef.InitVariable;
import src.AST.expression.*;
import src.AST.statement.Statement;
import src.AST.statement.loopStatement.ForLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Domain {
    public List<Index> indexList;
    public List<Assign> stmtList;
    private Stack<Object> result;
    private Coordinates curCoordinates;


    public Domain() {
        indexList = new ArrayList<>();
        stmtList = new ArrayList<>();
        result = new Stack<>();
        curCoordinates = new Coordinates();
    }

    public boolean getLoop(ForLoop forLoop) {
        // check init-exp
        if (forLoop.parallelExp != null) {
            return false;
        }
        if (forLoop.variableDef.initVariablelist.size() != 1) {
            return false;
        }
        InitVariable indexInit = forLoop.variableDef.initVariablelist.get(0);
        if (!getNumber(indexInit.exp)) {
            return false;// TODO: Extend the condition to Variable bound
        }
        String index = indexInit.variableName;
        long boundFrom = (long) result.pop();
        // check cond-exp
        long boundTo = 0;
        if (!(forLoop.conditionExp instanceof BinaryExp cond)) {
            return false;
        }
        int offsetBoundTo = 0; // TODO: Now only support like: i < const.
        switch (cond.op) {
            case "<" -> offsetBoundTo = -1;
            case ">" -> offsetBoundTo = 1;
            case "<=", ">=" -> {
            }
            default -> {
                return false;
            }
        }
        if (cond.lhs instanceof VariableLhsExp variable) {
            if (!(variable.variableName.equals(index))) {
                return false;
            }
            if (!getNumber(cond.rhs)) {
                return false;
            }
            boundTo = (long) result.pop() + offsetBoundTo;
        } else if (getNumber(cond.lhs)) {
            boundTo = (long) result.pop() - offsetBoundTo;
            if (!(cond.rhs instanceof VariableLhsExp variable)) {
                return false;
            }
            if (!(variable.variableName.equals(index))) {
                return false;
            }
        } else {
            return false;
        }

        // check step-exp
        long step = 0;
        if (forLoop.stepExp instanceof PrefixLhsExp stepExp) {
            if (!(stepExp.exp instanceof VariableLhsExp variable)) {
                return false;
            }
            if (!(variable.variableName.equals(index))) {
                return false;
            }
            step = stepExp.op.equals("++") ? 1 : -1;
        } else if (forLoop.stepExp instanceof
                PostfixExp stepExp) {
            if (!(stepExp.exp instanceof VariableLhsExp variable)) {
                return false;
            }
            if (!(variable.variableName.equals(index))) {
                return false;
            }
            step = stepExp.op.equals("++") ? 1 : -1;
        } else if (forLoop.stepExp instanceof AssignExp) {
            if (!(((AssignExp) forLoop.stepExp).lhs instanceof VariableLhsExp variable)) {
                return false;
            }
            if (!variable.variableName.equals(index)) {
                return false;
            }
            if (((AssignExp) forLoop.stepExp).rhs instanceof BinaryExp stepExp) {
                long add = 1;
                if (stepExp.op.equals("+")) {
                    add = 1;
                } else if (stepExp.op.equals("-")) {
                    add = -1;
                } else {
                    return false;
                }
                if (stepExp.lhs instanceof VariableLhsExp variableBin) {
                    if (!variableBin.variableName.equals(index)) {
                        return false;
                    }
                    if (!getNumber(stepExp.rhs)) {
                        return false;
                    }
                    step = add * (long) result.pop();
                } else if (getNumber(stepExp.lhs)) {
                    step = add * (long) result.pop();
                    if (add == -1) {
                        return false;
                    }
                    if (!(stepExp.rhs instanceof VariableLhsExp variableBin)) {
                        return false;
                    }
                    if (!variableBin.variableName.equals(index)) {
                        return false;
                    }
                }
            }
        }
        Index indexNew = new Index(index, boundFrom, boundTo, step);
        indexList.add(indexNew);
        curCoordinates.push(indexNew);
        // check body
        if (!getStmt(forLoop.stmt)) {
            return false;
        }
        curCoordinates.pop();
        curCoordinates.next();
        return true;
    }

    public boolean getStmt(Statement statement) {
        if (statement.suite != null) {
            for (var stmt : statement.suite.statementList) {
                if (!getStmt(stmt)) {
                    return false;
                }
            }
            return true;
        }
        if (statement.parallelExp != null) {
            for (var exp : statement.parallelExp.expList) {
                Assign assign = new Assign(curCoordinates);
                if (!getAssign(exp, assign)) {
                    return false;
                }
                curCoordinates.next();
                stmtList.add(assign);
            }
            return true;
        }
        if (statement.loopStatement != null) {
            return statement.loopStatement instanceof ForLoop && getLoop((ForLoop) statement.loopStatement);
        }
        return false;
    }

    public boolean getNumber(Expression exp) {
        if (exp instanceof NumberExp) {
            result.push(((NumberExp) exp).value);
            return true;
        } else if (exp instanceof UnaryExp unary) {
            if (!unary.op.equals("-")) {
                return false;
            }
            if (unary.exp instanceof NumberExp) {
                result.push(-((NumberExp) unary.exp).value);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean getAssign(Expression exp, Assign assign) {
        result.clear();
        if (exp instanceof PrefixLhsExp preExp) {
            if (!(preExp.exp instanceof ArrayElementLhsExp)) {
                return false;
            }
            MemVisit write = new MemVisit();
            if (!getMem(preExp.exp, write)) {
                return false;
            }
            assign.setWrite(write);
            assign.setRead(new MemVisit(write));
            return true;
        } else if (exp instanceof PostfixExp postExp) {
            if (!(postExp.exp instanceof ArrayElementLhsExp)) {
                return false;
            }
            MemVisit write = new MemVisit();
            if (!getMem(postExp.exp, write)) {
                return false;
            }
            assign.setWrite(write);
            assign.setRead(new MemVisit(write));
            return true;
        } else if (exp instanceof AssignExp) {
            if (!(((AssignExp) exp).lhs instanceof ArrayElementLhsExp)) {
                return false;
            }
            MemVisit write = new MemVisit();
            if (!getMem(((AssignExp) exp).lhs, write)) {
                return false;
            }
            assign.setWrite(write);
            return getAssignRhs(((AssignExp) exp).rhs, assign);
        }
        return false;
    }

    public boolean getAssignRhs(Expression exp, Assign assign) {
        if (exp instanceof ArrayElementLhsExp) {
            MemVisit read = new MemVisit();
            if (getMem(exp, read)) {
                assign.setRead(read);
                return true;
            }
            return false;
        }
        if (getNumber(exp)) {
            return true;
        }
        if (exp instanceof BinaryExp) {
            return getAssignRhs(((BinaryExp) exp).lhs, assign) && getAssignRhs(((BinaryExp) exp).rhs, assign);
        }
        return false;
    }


    public boolean getMem(Expression exp, MemVisit memVisit) {
        if (exp instanceof VariableLhsExp) {
            memVisit.setVarName(((VariableLhsExp) exp).variableName);
            return true;
        }
        if (exp instanceof ArrayElementLhsExp) {
            if (!getMem(((ArrayElementLhsExp) exp).variable, memVisit)) {
                return false;
            }
            Affine indexAffine = new Affine();
            if (!getAddrAffine(((ArrayElementLhsExp) exp).index, 1, indexAffine)) {
                return false;
            }
            memVisit.addDim(indexAffine);
            return true;
        }
        return false;
    }


    public boolean getAddrAffine(Expression exp, long constWeight, Affine affine) {
        if (getNumber(exp)) {
            affine.addBias(constWeight * (long) result.pop());
            return true;
        }
        if (exp instanceof VariableLhsExp) {
            affine.addVarCo(((VariableLhsExp) exp).variableName, constWeight);
            return true;
        }
        if (exp instanceof BinaryExp) {
            switch (((BinaryExp) exp).op) {
                case "+" -> {
                    return getAddrAffine(((BinaryExp) exp).lhs, constWeight, affine) &&
                            getAddrAffine(((BinaryExp) exp).rhs, constWeight, affine);
                }
                case "-" -> {
                    return getAddrAffine(((BinaryExp) exp).lhs, constWeight, affine) &&
                            getAddrAffine(((BinaryExp) exp).rhs, -constWeight, affine);
                }
                case "*" -> {
                    if (getNumber(((BinaryExp) exp).lhs)) {
                        Affine tmp = new Affine();
                        if (getAddrAffine(((BinaryExp) exp).rhs, constWeight * (long) result.pop(), tmp)) {
                            affine.merge(tmp, 1);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    if (getNumber(((BinaryExp) exp).rhs)) {
                        Affine tmp = new Affine();
                        if (getAddrAffine(((BinaryExp) exp).lhs, constWeight * (long) result.pop(), tmp)) {
                            affine.merge(tmp, 1);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    Affine tmpLhs = new Affine(), tmpRhs = new Affine();
                    if (getAddrAffine(((BinaryExp) exp).lhs, constWeight, tmpLhs) &&
                            getAddrAffine(((BinaryExp) exp).rhs, constWeight, tmpRhs)) {
                        if (tmpLhs.isConst()) {
                            affine.merge(tmpRhs, constWeight * tmpLhs.bias);
                            return true;
                        }
                        if (tmpRhs.isConst()) {
                            affine.merge(tmpLhs, constWeight * tmpRhs.bias);
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
                default -> {
                    return false;
                }
            }
        }
        return false;
    }

}
