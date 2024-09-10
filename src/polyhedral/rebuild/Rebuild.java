package src.polyhedral.rebuild;

import src.AST.ASTVisitor;
import src.AST.Program;
import src.AST.definition.*;
import src.AST.definition.variableDef.InitVariable;
import src.AST.definition.variableDef.VariableDef;
import src.AST.expression.*;
import src.AST.statement.Statement;
import src.AST.statement.Suite;
import src.AST.statement.jumpStatement.BreakStmt;
import src.AST.statement.jumpStatement.ContinueStmt;
import src.AST.statement.jumpStatement.ReturnStmt;
import src.AST.statement.loopStatement.ForLoop;
import src.AST.statement.loopStatement.WhileLoop;
import src.AST.statement.selectStatement.SelectStatement;
import src.Util.type.Type;
import src.polyhedral.dependency.Model;
import src.polyhedral.extract.Domain;
import src.polyhedral.matrix.Matrix;
import src.polyhedral.schedule.Schedule;

import java.util.HashSet;

import static java.lang.Math.abs;
import static src.polyhedral.matrix.Fraction.getDenominatorLCM;


public class Rebuild implements ASTVisitor {

    Schedule schedule = null;
    HashSet<String> already_index;
    int index = 0;
    int cnt = 0;

    public Rebuild(Program program) {
        already_index = new HashSet<>();
        visit(program);
    }

    @Override
    public void visit(Program node) {
        node.defList.forEach(def -> def.accept(this));
    }

    @Override
    public void visit(Definition node) {
        if (node.mainDef != null) {
            node.mainDef.accept(this);
        } else if (node.classDef != null) {
            node.classDef.accept(this);
        } else if (node.functionDef != null) {
            node.functionDef.accept(this);
        } else if (node.variableDef != null) {
            node.variableDef.accept(this);
        }
    }

    @Override
    public void visit(MainDef node) {
        System.out.print("int main()\n");
        node.suite.accept(this);
    }

    @Override
    public void visit(ClassDef node) {
        System.out.print("class ");
        System.out.print(node.className);
        System.out.print("\n{\n");
        node.variableDefList.forEach(varDef -> varDef.accept(this));
        node.constructor.accept(this);
        node.functionDefList.forEach(funcDef -> funcDef.accept(this));
        System.out.print("};\n");
    }

    @Override
    public void visit(Constructor node) {
        System.out.print(node.className);
        System.out.print("()\n");
        node.suite.accept(this);
    }

    @Override
    public void visit(FunctionDef node) {
        printType(node.type);
        System.out.print(" ");
        System.out.print(node.functionName);
        System.out.print("(");
        if (node.parameterNameList != null) {
            for (int i = 0; i < node.parameterNameList.size(); ++i) {
                printType(node.parameterTypeList.get(i));
                System.out.print(" ");
                System.out.print(node.parameterNameList.get(i));
                if (i != node.parameterNameList.size() - 1) {
                    System.out.print(", ");
                }
            }
        }
        System.out.print(")\n");
        node.body.accept(this);
    }

    @Override
    public void visit(VariableDef node) {
        node.initVariablelist.forEach(varDef -> varDef.accept(this));
    }

    @Override
    public void visit(InitVariable node) {
        printType(node.type);
        System.out.print(" ");
        System.out.print(node.variableName);
        if (node.exp != null) {
            System.out.print(" = ");
            node.exp.accept(this);
        }
        System.out.print(";\n");
    }

    @Override
    public void visit(Suite node) {
        System.out.print("{\n");
        node.statementList.forEach(stmt -> stmt.accept(this));
        System.out.print("}\n");
    }

    @Override
    public void visit(Statement node) {
        if (schedule != null) {
            for (int i = 0; i < schedule.hermite.row(); ++i) {
                String indexName = schedule.model.domain.indexList.get(i).varName;
                if (schedule.model.domain.variables.get(index).contains(indexName) && !already_index.contains(indexName)) {
                    System.out.print("int " + indexName + " = ");
                    rebuildTrans(i);
                    System.out.print(";\n");
                    already_index.add(indexName);
                }
            }
        }
        if (node.variableDef != null) {
            node.variableDef.accept(this);
        } else if (node.suite != null) {
            node.suite.accept(this);
        } else if (node.jumpStatement != null) {
            node.jumpStatement.accept(this);
        } else if (node.loopStatement != null) {
            node.loopStatement.accept(this);
        } else if (node.selectStatement != null) {
            node.selectStatement.accept(this);
        } else if (node.parallelExp != null) {
            node.parallelExp.accept(this);
        }
    }

    @Override
    public void visit(SelectStatement node) {
        System.out.print("if (");
        node.judgeExp.accept(this);
        System.out.print(")\n");
        node.trueStmt.accept(this);
        System.out.print("else\n");
        node.falseStmt.accept(this);
    }

    @Override
    public void visit(ForLoop node) {
        boolean rebuild = false;
        try {
            if (schedule == null) {
                Domain domain = new Domain();
                if (domain.getLoop(node)) {
                    Model model = new Model(domain);
                    if (model.setDependency()) {
                        schedule = new Schedule(model);
                        index = -1;
                        rebuild = true;
                        already_index.clear();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        if (schedule != null) {
            ++index;
            String rebuildLow = rebuildLowerBound(index);
            String rebuildUp = rebuildUpperBound(index);
            String rebuildIndex = "rebuildLoopIndex_f" + index;
            System.out.print("for(int " + rebuildIndex + " = " + rebuildLow + "; ");
            System.out.print(rebuildIndex + " <= " + rebuildUp + "; ");
            System.out.print(rebuildIndex + " = " + rebuildIndex + " + " + schedule.hermite.getElement(index, index) + "){\n");
            node.stmt.accept(this);
            System.out.print("}\n");
        }
        if (rebuild) {
            schedule = null;
            index = -1;
            already_index.clear();
        }
    }

    @Override
    public void visit(WhileLoop node) {
        System.out.print("while(");
        node.judgeExp.accept(this);
        System.out.print(")\n");
        node.stmt.accept(this);
    }

    @Override
    public void visit(BreakStmt node) {
        System.out.print("break;\n");
    }

    @Override
    public void visit(ContinueStmt node) {
        System.out.print("continue;\n");
    }

    @Override
    public void visit(ReturnStmt node) {
        System.out.print("return ");
        if (node.returnExp != null) {
            node.returnExp.accept(this);
        }
        System.out.print(";\n");
    }

    @Override
    public void visit(ParallelExp node) {
        node.expList.forEach(exp -> {
            exp.accept(this);
            System.out.print(";\n");
        });
    }

    @Override
    public void visit(ClassMemberLhsExp node) {
        printType(node.type);
        System.out.print(" ");
        System.out.print(node.memberName);
        if (node.classVariable != null) {
            System.out.print(" = ");
            node.classVariable.accept(this);
        }
        System.out.print(";\n");
    }

    @Override
    public void visit(ClassMemFunctionLhsExp node) {
        node.classVariable.accept(this);
        System.out.print(".");
        System.out.print(node.memberFuncName);
        System.out.print("(");
        if (node.callList.expList != null) {
            for (int i = 0; i < node.callList.expList.size(); ++i) {
                node.callList.expList.get(i).accept(this);
                if (i != node.callList.expList.size() - 1) {
                    System.out.print(", ");
                }
            }
        }
        System.out.print(")");
    }

    @Override
    public void visit(FunctionCallLhsExp node) {
        System.out.print(node.functionName);
        System.out.print("(");
        if (node.callExpList != null) {
            for (int i = 0; i < node.callExpList.expList.size(); ++i) {
                node.callExpList.expList.get(i).accept(this);
                if (i != node.callExpList.expList.size() - 1) {
                    System.out.print(", ");
                }
            }
        }
        System.out.print(")");
    }

    @Override
    public void visit(ArrayElementLhsExp node) {
        node.variable.accept(this);
        System.out.print("[");
        node.index.accept(this);
        System.out.print("]");
    }

    @Override
    public void visit(AssignExp node) {
        node.lhs.accept(this);
        System.out.print(" = ");
        node.rhs.accept(this);
    }

    @Override
    public void visit(BinaryExp node) {
        node.lhs.accept(this);
        System.out.print(" ");
        System.out.print(node.op);
        System.out.print(" ");
        node.rhs.accept(this);
    }

    @Override
    public void visit(NewArrayExp node) {
        System.out.print("new ");
        System.out.print(node.type.GetType());
        for (int i = 0; i < node.expressionList.size(); ++i) {
            System.out.print("[");
            node.expressionList.get(i).accept(this);
            System.out.print("]");
        }
    }

    @Override
    public void visit(NewClassExp node) {
        System.out.print("new ");
        printType(node.type);
        System.out.print("()");
    }

    @Override
    public void visit(PostfixExp node) {
        node.exp.accept(this);
        System.out.print(node.op);
    }

    @Override
    public void visit(PrefixLhsExp node) {
        System.out.print(node.op);
        node.exp.accept(this);
    }

    @Override
    public void visit(TernaryExp node) {
        node.condition.accept(this);
        System.out.print(" ? ");
        node.trueExp.accept(this);
        System.out.print(" : ");
        node.falseExp.accept(this);
    }

    @Override
    public void visit(UnaryExp node) {
        System.out.print(node.op);
        node.exp.accept(this);
    }

    @Override
    public void visit(VariableLhsExp node) {
        System.out.print(node.variableName);
    }

    @Override
    public void visit(ThisPointerExp node) {
        System.out.print("this");
    }

    @Override
    public void visit(BoolExp node) {
        System.out.print(node.value);
    }

    @Override
    public void visit(NumberExp node) {
        System.out.print(node.value);
    }

    @Override
    public void visit(StringExp node) {
        System.out.print(node.value);
    }

    @Override
    public void visit(NullExp node) {
        System.out.print("null");
    }

    void printType(Type type) {
        System.out.print(type.GetType());
        for (int i = 0; i < type.dim; ++i) {
            System.out.print("[]");
        }
    }

    public String rebuildLowerBound(int num) {
        String rebuildLow = "rebuildLow_" + cnt++;
        System.out.print("int " + rebuildLow + " = -2147483648;\n");
        var list = schedule.fourierMotzkin.lowerBound.get("f" + num);
        for (src.polyhedral.schedule.AffineFraction affine : list) {
            String rebuildTmp = "rebuildTmp_" + cnt++;
            System.out.print("int " + rebuildTmp + " = ");
            affine.rebuild(true); // ceil
            System.out.print(";\n");
            System.out.print(rebuildTmp + " = " + rebuildTmp + " + (");
            rebuildMatrixCoe(schedule.hermite, num);
            System.out.print(" - " + rebuildTmp);
            System.out.print(") % ");
            System.out.print(schedule.hermite.getElement(num, num));
            System.out.print(";\n");
            System.out.print(rebuildLow + " = " + rebuildLow + " < " + rebuildTmp + " ? " + rebuildTmp + " : " + rebuildLow + ";\n");
        }
        return rebuildLow;
    }

    public String rebuildUpperBound(int num) {
        String rebuildUp = "rebuildUp_" + cnt++;
        System.out.print("int " + rebuildUp + " = 2147483647;\n");
        var list = schedule.fourierMotzkin.upperBound.get("f" + num);
        for (src.polyhedral.schedule.AffineFraction affine : list) {
            String rebuildTmp = "rebuildTmp_" + cnt++;
            System.out.print("int " + rebuildTmp + " = ");
            affine.rebuild(false); // floor
            System.out.print(";\n");
            System.out.print(rebuildUp + " = " + rebuildUp + " < " + rebuildTmp + " ? " + rebuildUp + " : " + rebuildTmp + ";\n");
        }
        return rebuildUp;
    }

    public void rebuildMatrixCoe(Matrix matrix, int num) {
        if (num == 0) {
            return;
        }
        long de = 1;
        for (int i = 0; i < num; ++i) {
            de = getDenominatorLCM(de, matrix.getElement(num, i).denominator());
        }
        de = abs(de);
        System.out.print("(");
        for (int i = 0; i < num; ++i) {
            if (i != 0) {
                System.out.print(" + ");
            }
            System.out.print(de * matrix.getElement(num, i).numerator() / matrix.getElement(num, i).denominator());
            System.out.print("*");
            System.out.print("rebuildLoopIndex_f" + i);
        }
        System.out.print(")/");
        System.out.print(de);
    }

    public void rebuildTrans(int num) {
        long de = 1;
        for (int i = 0; i < schedule.transformInverse.row(); ++i) {
            de = getDenominatorLCM(de, schedule.transformation.getElement(num, i).denominator());
        }
        de = abs(de);
        System.out.print("(");
        for (int i = 0; i < schedule.transformInverse.row(); ++i) {
            if (i != 0) {
                System.out.print(" + ");
            }
            long tmp = de * schedule.transformInverse.getElement(num, i).numerator() / schedule.transformInverse.getElement(num, i).denominator();
            if (tmp != 0) {
                System.out.print(tmp);
                System.out.print("*");
                System.out.print("rebuildLoopIndex_f" + i);
            } else {
                System.out.print(0);
            }
        }
        System.out.print(")/");
        System.out.print(de);
    }
}
