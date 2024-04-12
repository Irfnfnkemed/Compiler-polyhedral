package src.IR;

import src.IR.statement.*;

import java.util.ArrayList;
import java.util.List;

public class IRProgram extends IRNode {
    public List<IRStatement> stmtList;
    public int classDefIndex = 1;
    public int varDefIndex = 1;

    public IRProgram() {
        stmtList = new ArrayList<>();
        stmtList.add(new ConstString());
    }

    public void push(IRStatement stmt) {
        if (stmt instanceof ClassTypeDef) {
            stmtList.add(classDefIndex++, stmt);
            ++varDefIndex;
        } else if (stmt instanceof GlobalVarDef) {
            stmtList.add(varDefIndex++, stmt);
        } else if (stmt instanceof FuncDef) {
            stmtList.add(stmt);
        }
    }

    public String pushConstString(String content) {
        return ((ConstString) stmtList.get(0)).push(content);
    }
}
