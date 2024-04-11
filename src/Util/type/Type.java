package src.Util.type;

import src.Util.position.Position;

import java.util.Objects;

import static src.Util.type.Type.TypeEnum.*;

public class Type {
    public enum TypeEnum {
        VOID, BOOL, INT, STRING, CLASS, NULL, UNKNOWN;
    }

    public Position position;
    public TypeEnum typeEnum;
    public String typeName;
    public int dim;

    public Type() { //默认
        typeEnum = UNKNOWN;
        typeName = "";
        dim = 0;
    }

    public Type(Type obj) { //拷贝
        typeEnum = obj.typeEnum;
        typeName = obj.typeName;
        dim = obj.dim;
    }

    public Type setVoid() {
        typeEnum = VOID;
        return this;
    }

    public Type setInt() {
        typeEnum = INT;
        return this;
    }

    public Type setBool() {
        typeEnum = BOOL;
        return this;
    }

    public Type setString() {
        typeEnum = STRING;
        return this;
    }

    public Type setClass(String name) {
        typeEnum = CLASS;
        typeName = name;
        return this;
    }

    public Type setNull() {
        typeEnum = NULL;
        return this;
    }

    public Type setArray(Type typeBase, int dim_) {
        typeEnum = typeBase.typeEnum;
        typeName = typeBase.typeName;
        dim = dim_;
        return this;
    }

    public String GetType() {
        if (!Objects.equals(typeName, "")) {
            return typeName;
        } else {
            return switch (typeEnum) {
                case VOID -> "void";
                case INT -> "int";
                case BOOL -> "bool";
                case STRING -> "string";
                case CLASS -> typeName;
                case NULL -> "null";
                default -> "";
            };
        }
    }

    public boolean isArray() {
        return dim > 0;
    }

    public boolean isInt() {
        return !isArray() && typeEnum == INT;
    }

    public boolean isBool() {
        return !isArray() && typeEnum == BOOL;
    }

    public boolean isString() {
        return !isArray() && typeEnum == STRING;
    }

    public boolean isVoid() {
        return !isArray() && typeEnum == VOID;
    }

    public boolean isClass() {
        return !isArray() && typeEnum == CLASS;
    }

    public boolean isNull() {
        return !isArray() && typeEnum == NULL;
    }

    public boolean assign(Type type) {
        if (typeEnum == NULL) {
            return type.typeEnum == CLASS || type.dim > 0 || type.typeEnum == NULL;
        } else if (type.typeEnum == NULL) {
            return typeEnum == CLASS || dim > 0 || typeEnum == NULL;
        } else {
            return typeEnum == type.typeEnum && dim == type.dim && Objects.equals(typeName, type.typeName);
        }
    }

    public boolean compare(Type type) {
        if (typeEnum == NULL) {
            return type.typeEnum == CLASS || type.dim > 0 || type.typeEnum == NULL;
        } else if (type.typeEnum == NULL) {
            return typeEnum == CLASS || dim > 0 || typeEnum == NULL;
        } else if (type.dim > 0) {
            return false;
        } else {
            return typeEnum == type.typeEnum && Objects.equals(typeName, type.typeName);
        }
    }

    public static Type getCommon(Type typeLhs, Type typeRhs) {
        if (typeLhs.isNull()) {
            return typeRhs;
        } else {
            return typeLhs;
        }
    }
}
