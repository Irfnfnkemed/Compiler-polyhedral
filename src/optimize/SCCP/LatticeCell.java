package src.optimize.SCCP;

import src.Util.cell.Cell;

import static java.lang.Math.min;

public class LatticeCell {
    public static final int UNDEFINED = 3;
    public static final int CONST = 2;
    public static final int UNCERTAIN_COPY = 1;
    public static final int UNCERTAIN_NEW = 0;

    public long constValue;
    public String varCopy = null;
    public String varNew = null;
    public int status;

    public LatticeCell(String varNew_) {
        init(varNew_);
    }

    public LatticeCell(long value_) {
        init(value_);
    }

    public LatticeCell(Cell cell_) {
        if (cell_.isConst) {
            init(cell_.varValue);
        } else {
            init(cell_.varName);
        }
    }

    void init(String varNew_) {
        if (varNew_.charAt(0) == '@') {
            status = UNCERTAIN_NEW;
        } else {
            status = UNDEFINED;
        }
        varNew = varNew_;
    }

    void init(long value) {
        status = CONST;
        constValue = value;
    }

    public boolean update(LatticeCell operand) {
        int tmp = status;
        if (status == UNDEFINED) {
            if (operand.status == CONST) {
                constValue = operand.constValue;
                status = CONST;
            } else if (operand.status == UNCERTAIN_COPY) {
                varCopy = operand.varCopy;
                status = UNCERTAIN_COPY;
            } else if (operand.status == UNCERTAIN_NEW) {
                varCopy = operand.varNew;
                status = UNCERTAIN_COPY;
            }
        } else if (status == CONST) {
            if (operand.status == CONST) {
                if (constValue != operand.constValue) {
                    status = UNCERTAIN_NEW;
                }
            } else if (operand.status == UNCERTAIN_COPY || operand.status == UNCERTAIN_NEW) {
                status = UNCERTAIN_NEW;
            }
        } else if (status == UNCERTAIN_COPY) {
            if (operand.status == CONST) {
                status = UNCERTAIN_NEW;
            } else if (operand.status == UNCERTAIN_COPY) {
                if (!varCopy.equals(operand.varCopy)) {
                    status = UNCERTAIN_NEW;
                }
            } else if (operand.status == UNCERTAIN_NEW) {
                if (!varCopy.equals(operand.varNew)) {
                    status = UNCERTAIN_NEW;
                }
            }
        }
        return status != tmp;
    }

    public boolean update(LatticeCell lhs, LatticeCell rhs, String op) {
        int tmp = status;
        if (lhs.status == CONST && rhs.status == CONST) {
            status = CONST;
            try {
                switch (op) {
                    case "add" -> constValue = lhs.constValue + rhs.constValue;
                    case "sub" -> constValue = lhs.constValue - rhs.constValue;
                    case "mul" -> constValue = lhs.constValue * rhs.constValue;
                    case "sdiv" -> constValue = lhs.constValue / rhs.constValue;
                    case "srem" -> constValue = lhs.constValue % rhs.constValue;
                    case "shl" -> constValue = lhs.constValue << rhs.constValue;
                    case "ashr" -> constValue = lhs.constValue >> rhs.constValue;
                    case "and" -> constValue = lhs.constValue & rhs.constValue;
                    case "or" -> constValue = lhs.constValue | rhs.constValue;
                    case "xor" -> constValue = lhs.constValue ^ rhs.constValue;
                    case "slt" -> constValue = (lhs.constValue < rhs.constValue) ? 1 : 0;
                    case "sgt" -> constValue = (lhs.constValue > rhs.constValue) ? 1 : 0;
                    case "sle" -> constValue = (lhs.constValue <= rhs.constValue) ? 1 : 0;
                    case "sge" -> constValue = (lhs.constValue >= rhs.constValue) ? 1 : 0;
                    case "eq" -> constValue = (lhs.constValue == rhs.constValue) ? 1 : 0;
                    case "ne" -> constValue = (lhs.constValue != rhs.constValue) ? 1 : 0;
                }
            } catch (Exception exception) { //直接计算会抛出错误(除以0，位移负数等)
                status = UNCERTAIN_NEW;
            }
        } else {
            status = min(lhs.status, rhs.status);
        }
        if (status <= UNCERTAIN_COPY) {
            status = UNCERTAIN_NEW;
        }
        return status != tmp;
    }

    public boolean removed() {
        return status != UNCERTAIN_NEW;
    }

    public String getVar() {
        if (status == UNCERTAIN_COPY) {
            return varCopy;
        } else if (status == UNCERTAIN_NEW) {
            return varNew;
        } else {
            return null;
        }
    }
}
