package src.Util.type;

public class IRType {
    public String unitName;
    public int unitSize;//0表待定
    public boolean isArray = false;

    public IRType(Type type) {
        if (type.dim > 0) {
            isArray = true;
            if (type.dim > 1) {
                unitName = "ptr";
                unitSize = 0;
                return;
            }
        }
        if (type.typeEnum == Type.TypeEnum.BOOL) {
            unitName = "i1";
            unitSize = 1;
        } else if (type.typeEnum == Type.TypeEnum.VOID) {
            unitName = "void";
            unitSize = -1;
        } else if (type.typeEnum == Type.TypeEnum.INT) {
            unitName = "i32";
            unitSize = 32;
        } else {
            unitName = "ptr";
            unitSize = 32;
        }
    }

    public IRType(IRType irType) {
        unitName = irType.unitName;
        unitSize = irType.unitSize;
        isArray = irType.isArray;
    }

    public IRType() {
    }

    public IRType setI32() {
        unitName = "i32";
        unitSize = 32;
        return this;
    }

    public IRType setI1() {
        unitName = "i1";
        unitSize = 1;
        return this;
    }

    public IRType setVoid() {
        unitName = "void";
        unitSize = -1;
        return this;
    }

    public IRType setPtr() {
        unitName = "ptr";
        unitSize = 32;
        return this;
    }

    public IRType setClass(String className) {
        unitName = "%class-" + className;
        unitSize = 0;
        return this;
    }


}
