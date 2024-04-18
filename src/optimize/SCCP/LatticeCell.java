package src.optimize.SCCP;

import src.IR.instruction.Instruction;
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
    public long imme_add = 0, imme_mul = 1;
    public int status;

    public LatticeCell(String varNew_) {
        init(varNew_);
    }

    public LatticeCell(long value_) {
        init(value_);
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
        int tmpStatus = status;
        long tmpAdd = imme_add, tmpMul = imme_mul;
        if (status == UNDEFINED) {
            if (operand.status == CONST) {
                constValue = operand.constValue;
                status = CONST;
            } else if (operand.status == UNCERTAIN_COPY) {
                varCopy = operand.varCopy;
                imme_add = operand.imme_add;
                imme_mul = operand.imme_mul;
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
                if (!(varCopy.equals(operand.varCopy) && imme_add == operand.imme_add && imme_mul == operand.imme_mul)) {
                    status = UNCERTAIN_NEW;
                }
            } else if (operand.status == UNCERTAIN_NEW) {
                if (!(varCopy.equals(operand.varNew) && imme_add == 0 && imme_mul == 1)) {
                    status = UNCERTAIN_NEW;
                }
            }
        }
        return status != tmpStatus;
    }

    public boolean update(LatticeCell lhs, LatticeCell rhs, String op) {
        if (lhs.status == UNDEFINED || rhs.status == UNDEFINED) {
            return false;//存在未访问的，暂不操作（因为此函数针对binary、icmp，两操作数均需初始化）
        }
        int tmpStatus = status;
        long tmpAdd = imme_add, tmpMul = imme_mul;
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
        } else if (rhs.status == CONST) {
            switch (op) {
                case "add" -> {
                    status = UNCERTAIN_COPY;
                    if (lhs.status == UNCERTAIN_COPY) {
                        varCopy = lhs.varCopy;
                        imme_mul = lhs.imme_mul;
                        imme_add = lhs.imme_add + rhs.constValue;
                    } else {
                        varCopy = lhs.varNew;
                        imme_add = rhs.constValue;
                    }
                }
                case "sub" -> {
                    status = UNCERTAIN_COPY;
                    if (lhs.status == UNCERTAIN_COPY) {
                        varCopy = lhs.varCopy;
                        imme_mul = lhs.imme_mul;
                        imme_add = lhs.imme_add - rhs.constValue;
                    } else {
                        varCopy = lhs.varNew;
                        imme_add = -rhs.constValue;
                    }
                }
                case "mul" -> {
                    if (rhs.constValue == 0) {
                        status = CONST;
                        constValue = 0;
                    } else {
                        if (lhs.status == UNCERTAIN_COPY) {
                            varCopy = lhs.varCopy;
                            imme_mul = lhs.imme_mul * rhs.constValue;
                            imme_add = lhs.imme_add * rhs.constValue;
                        } else {
                            varCopy = lhs.varNew;
                            imme_mul = rhs.constValue;
                        }
                        status = UNCERTAIN_COPY;
                    }
                }
                default -> status = UNCERTAIN_NEW;
            }
        } else if (lhs.status == CONST) {
            switch (op) {
                case "add" -> {
                    status = UNCERTAIN_COPY;
                    if (rhs.status == UNCERTAIN_COPY) {
                        varCopy = rhs.varCopy;
                        imme_mul = rhs.imme_mul;
                        imme_add = rhs.imme_add + lhs.constValue;
                    } else {
                        varCopy = rhs.varNew;
                        imme_add = lhs.constValue;
                    }
                }
                case "sub" -> {
                    status = UNCERTAIN_COPY;
                    if (rhs.status == UNCERTAIN_COPY) {
                        varCopy = rhs.varCopy;
                        imme_mul = -rhs.imme_mul;
                        imme_add = -rhs.imme_add + lhs.constValue;
                    } else {
                        varCopy = rhs.varNew;
                        imme_mul = -1;
                        imme_add = lhs.constValue;
                    }
                }
                case "mul" -> {
                    if (lhs.constValue == 0) {
                        status = CONST;
                        constValue = 0;
                    } else {
                        if (rhs.status == UNCERTAIN_COPY) {
                            varCopy = rhs.varCopy;
                            imme_mul = rhs.imme_mul * lhs.constValue;
                            imme_add = rhs.imme_add * lhs.constValue;
                        } else {
                            varCopy = rhs.varNew;
                            imme_mul = lhs.constValue;
                        }
                        status = UNCERTAIN_COPY;
                    }
                }
                default -> status = UNCERTAIN_NEW;
            }
        } else {
            status = UNCERTAIN_NEW;
        }
        return status != tmpStatus || imme_add != tmpAdd || imme_mul != tmpMul;
    }

}
