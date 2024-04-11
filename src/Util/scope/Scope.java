package src.Util.scope;

import src.Util.error.SemanticErrors;
import src.Util.position.Position;
import src.Util.type.Type;

import java.util.HashMap;

public class Scope {
    public static class Variable {
        public Type type;
        public boolean cover;//最多覆盖一次
        public int line = 0, column = 0;//位置，方便IR处理局部变量覆盖

        Variable(Type type_, boolean cover_, int line_, int column_) {
            type = type_;
            cover = cover_;
            line = line_;
            column = column_;
        }
    }

    public HashMap<String, Variable> variable;

    public boolean isGlobal = false;
    public boolean isClass = false;
    public boolean isFunction = false;
    public boolean isConstructor = false;
    public Position loopPos = null;
    public boolean notReturn = true;//用于isFunction为true，表示函数可能还未返回
    public Type returnType;//用于isFunction为true，处理return
    public Type classType;//用于isClass为true，处理this

    public Scope() {
        variable = new HashMap<>();
    }

    public Scope(Scope fatherScope) { //从父作用域构造出子作用域
        variable = new HashMap<>();
        fatherScope.variable.forEach((key, value) -> {
            variable.put(key, new Variable(new Type(value.type), false, value.line, value.column));
        });
        isGlobal = fatherScope.isGlobal;
        isClass = fatherScope.isClass;
        loopPos = fatherScope.loopPos;
        isFunction = fatherScope.isFunction;
        notReturn = fatherScope.notReturn;
        if (fatherScope.isClass) {
            classType = new Type(fatherScope.classType);
        }
        if (fatherScope.isFunction) {
            returnType = new Type(fatherScope.returnType);
        }
    }

    public Type getFoundation(Type.TypeEnum typeEnum) {
        return switch (typeEnum) {
            case VOID -> new Type().setVoid();
            case BOOL -> new Type().setBool();
            case INT -> new Type().setInt();
            case STRING -> new Type().setString();
            case NULL -> new Type().setNull();
            default -> null;
        };
    }

    public void setVariable(String variableName, Type variableType, Position position) {
        if (variable.containsKey(variableName) && variable.get(variableName).cover) {
            throw new SemanticErrors("Duplicate definition of variable.", position);
        } else {
            variable.put(variableName, new Variable(new Type(variableType), true, position.line, position.column));//覆盖或者添加
        }
    }

    public Variable getVariable(String variableName) {
        return variable.get(variableName);
    }
}
