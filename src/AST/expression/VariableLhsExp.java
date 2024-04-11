package src.AST.expression;

public class VariableLhsExp extends Expression {
    public String variableName;
    public int line = 0, column = 0;//定义时的位置
    public int id = -1;//若为类成员变量，表示类内变量下标；若为-1，表非类内变量

    public VariableLhsExp(String variableName_) {
        variableName = variableName_;
    }
}
