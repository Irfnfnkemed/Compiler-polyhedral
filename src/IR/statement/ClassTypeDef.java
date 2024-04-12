package src.IR.statement;

import src.AST.definition.variableDef.VariableDef;

import java.util.ArrayList;
import java.util.List;

public class ClassTypeDef extends IRStatement {
    public String className;
    public int classMemNum;
    public List<Boolean> isPtrList;
    public List<VariableDef> variableDefList;

    public ClassTypeDef(String className_, List<VariableDef> variableDefList_) {
        className = "%class-" + className_;
        isPtrList = new ArrayList<>();
        variableDefList = variableDefList_;
        variableDefList_.forEach(variableDef -> variableDef.initVariablelist.forEach(
                initVariable -> {
                    if (initVariable.type.isBool() || initVariable.type.isInt()) {
                        isPtrList.add(false);
                    } else {
                        isPtrList.add(true);
                    }
                }));
        classMemNum = isPtrList.size();
    }

}
