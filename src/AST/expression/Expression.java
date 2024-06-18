package src.AST.expression;

import src.AST.ASTNode;
import src.AST.ASTVisitor;
import src.Util.type.Type;

public class Expression extends ASTNode {
    public Type type;

    public boolean isAssign = false;

    Expression() {
        type = new Type();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        if (this instanceof VariableLhsExp) {
            visitor.visit((VariableLhsExp) this);
        } else if (this instanceof ThisPointerExp) {
            visitor.visit((ThisPointerExp) this);
        } else if (this instanceof NumberExp) {
            visitor.visit((NumberExp) this);
        } else if (this instanceof StringExp) {
            visitor.visit((StringExp) this);
        } else if (this instanceof BoolExp) {
            visitor.visit((BoolExp) this);
        } else if (this instanceof NullExp) {
            visitor.visit((NullExp) this);
        } else if (this instanceof ClassMemberLhsExp) {
            visitor.visit((ClassMemberLhsExp) this);
        } else if (this instanceof ClassMemFunctionLhsExp) {
            visitor.visit((ClassMemFunctionLhsExp) this);
        } else if (this instanceof ArrayElementLhsExp) {
            visitor.visit((ArrayElementLhsExp) this);
        } else if (this instanceof FunctionCallLhsExp) {
            visitor.visit((FunctionCallLhsExp) this);
        } else if (this instanceof PostfixExp) {
            visitor.visit((PostfixExp) this);
        } else if (this instanceof PrefixLhsExp) {
            visitor.visit((PrefixLhsExp) this);
        } else if (this instanceof UnaryExp) {
            visitor.visit((UnaryExp) this);
        } else if (this instanceof BinaryExp) {
            visitor.visit((BinaryExp) this);
        } else if (this instanceof TernaryExp) {
            visitor.visit((TernaryExp) this);
        } else if (this instanceof AssignExp) {
            visitor.visit((AssignExp) this);
        } else if (this instanceof NewClassExp) {
            visitor.visit((NewClassExp) this);
        } else if (this instanceof NewArrayExp) {
            visitor.visit((NewArrayExp) this);
        }
    }
}
