package src.AST.statement.loopStatement;

import src.AST.expression.Expression;
import src.AST.expression.ParallelExp;
import src.AST.statement.Statement;
import src.AST.definition.variableDef.VariableDef;

public class ForLoop extends LoopStatement {
    public ParallelExp parallelExp;
    public VariableDef variableDef;
    public Expression conditionExp;
    public Expression stepExp;
    public Statement stmt;
}
