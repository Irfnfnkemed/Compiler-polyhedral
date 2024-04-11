package src.AST.statement;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.AST.expression.ParallelExp;
import src.AST.statement.jumpStatement.JumpStatement;
import src.AST.statement.loopStatement.LoopStatement;
import src.AST.statement.selectStatement.SelectStatement;
import src.AST.definition.variableDef.VariableDef;

public class Statement extends ASTNode {
    public Suite suite;
    public SelectStatement selectStatement;
    public LoopStatement loopStatement;
    public JumpStatement jumpStatement;
    public VariableDef variableDef;
    public ParallelExp parallelExp;

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
