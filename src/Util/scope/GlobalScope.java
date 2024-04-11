package src.Util.scope;

import src.Util.error.SemanticErrors;
import src.Util.position.Position;
import src.Util.type.Type;

import java.util.*;

import static src.Util.type.Type.TypeEnum.*;

public class GlobalScope extends Scope {
    public static class FunctionTypes {
        public Type type; //方法返回值类型
        public List<Type> parameterTypes; //参数类型

        FunctionTypes() {
            parameterTypes = new ArrayList<>();
        }
    }

    public static class ClassMemId {
        public HashMap<String, Integer> ids;
        public int memberNum = 0;

        public int push(String memVar) {
            ids.put(memVar, memberNum);
            return memberNum++;
        }

        public ClassMemId() {
            ids = new HashMap<>();
        }
    }

    public HashSet<String> classNames;//类名
    public HashMap<String, HashMap<String, Type>> classMember; //类名->类的成员变量(变量名->类型)
    public HashMap<String, ClassMemId> classMemberId; //类名->类的成员变量下标
    public HashMap<String, HashMap<String, FunctionTypes>> classMethod; //类名->类的方法(方法名->返回类型与参数列表)
    public HashMap<String, FunctionTypes> function; // 函数名-> 返回类型与参数列表

    public GlobalScope() {
        super();
        classNames = new HashSet<>();
        classMember = new HashMap<>();
        classMemberId = new HashMap<>();
        classMethod = new HashMap<>();
        function = new HashMap<>();
        setFunction("print", getFoundation(VOID), setList(getFoundation(STRING)), null);
        setFunction("println", getFoundation(VOID), setList(getFoundation(STRING)), null);
        setFunction("printInt", getFoundation(VOID), setList(getFoundation(INT)), null);
        setFunction("printlnInt", getFoundation(VOID), setList(getFoundation(INT)), null);
        setFunction("getString", getFoundation(STRING), setList(), null);
        setFunction("getInt", getFoundation(INT), setList(), null);
        setFunction("toString", getFoundation(STRING), setList(getFoundation(INT)), null);
        setClassMethod("string", "length", getFoundation(INT), setList(), null);
        setClassMethod("string", "substring", getFoundation(STRING),
                setList(getFoundation(INT), getFoundation(INT)), null);
        setClassMethod("string", "parseInt", getFoundation(INT), setList(), null);
        setClassMethod("string", "ord", getFoundation(INT), setList(getFoundation(INT)), null);
    }

    public void setClassName(String className, Position position) {
        if (function.containsKey(className)) {
            throw new SemanticErrors("Duplicate class name and function name.", position);
        }
        if (classNames.contains(className)) {
            throw new SemanticErrors("Duplicate class name.", position);
        }
        classNames.add(className);
    }

    public void setClassMember(String className, String variableName, Type variableType, Position position) {
        if (function.containsKey(className)) {
            throw new SemanticErrors("Duplicate variable name and function name.", position);
        }
        if (getClassMethod(className, variableName) != null) {
            throw new SemanticErrors("Duplicate class member name and function name.", position);
        }
        var classMembers = classMember.get(className);
        if (classMembers != null) {
            var classMem = classMembers.get(variableName);
            if (classMem != null) {
                throw new SemanticErrors("Duplicate definition of class member variable.", position);
            } else {
                classMembers.put(variableName, new Type(variableType));
            }
            classMemberId.get(className).push(variableName);
        } else {
            HashMap<String, Type> classMem = new HashMap<>();
            classMem.put(variableName, new Type(variableType));
            classMember.put(className, classMem);
            ClassMemId classMemId = new ClassMemId();
            classMemberId.put(className, classMemId);
            classMemId.push(variableName);
        }
    }

    public void setClassMethod(String className, String methodName, Type returnType,
                               List<Type> parameterTypes, Position position) {
        var classMethods = classMethod.get(className);
        if (getClassMember(className, methodName) != null) {
            throw new SemanticErrors("Duplicate class member name and function name.", position);
        }
        if (classMethods != null) {
            var classMeth = classMethods.get(methodName);
            if (classMeth != null) {
                throw new SemanticErrors("Duplicate definition of class method.", position);
            } else {
                FunctionTypes functionTypes = new FunctionTypes();
                functionTypes.type = new Type(returnType);
                for (Type type : parameterTypes) {
                    functionTypes.parameterTypes.add(new Type(type));
                }
                classMethods.put(methodName, functionTypes);
            }
        } else {
            HashMap<String, FunctionTypes> classMeth = new HashMap<>();
            FunctionTypes functionTypes = new FunctionTypes();
            functionTypes.type = new Type(returnType);
            for (Type type : parameterTypes) {
                functionTypes.parameterTypes.add(new Type(type));
            }
            classMeth.put(methodName, functionTypes);
            classMethod.put(className, classMeth);
        }
    }

    public void setFunction(String funcName, Type returnType, List<Type> parameterTypes, Position position) {
        if (variable.containsKey(funcName)) {
            throw new SemanticErrors("Duplicate variable name and function name.", position);
        }
        if (classIsExist(funcName)) {
            throw new SemanticErrors("Duplicate function name and class name.", position);
        }
        if (function.containsKey(funcName)) {
            throw new SemanticErrors("Duplicate function name.", position);
        }
        FunctionTypes functionTypes = new FunctionTypes();
        functionTypes.type = new Type(returnType);
        for (Type type : parameterTypes) {
            functionTypes.parameterTypes.add(new Type(type));
        }
        function.put(funcName, functionTypes);
    }

    @Override
    public void setVariable(String variableName, Type variableType, Position position) {
        if (function.containsKey(variableName)) {
            throw new SemanticErrors("Duplicate variable name and function name.", position);
        }
        super.setVariable(variableName, variableType, position);
    }

    public boolean classIsExist(String className) {
        return classNames.contains(className);
    }

    public Type getClassMember(String className, String variableName) {
        var classMembers = classMember.get(className);
        if (classMembers != null) {
            return classMembers.get(variableName);
        } else {
            return null;
        }
    }

    public int getClassSize(String className) {
        var classMemIds = classMemberId.get(className);
        if (classMemIds != null) {
            return classMemIds.memberNum;
        } else {
            return 0;
        }
    }

    public int getClassMemberId(String className, String variableName) {
        return classMemberId.get(className).ids.get(variableName);
    }

    public FunctionTypes getClassMethod(String className, String methodName) {
        var classMethods = classMethod.get(className);
        if (classMethods != null) {
            return classMethods.get(methodName);
        } else {
            return null;
        }
    }

    public FunctionTypes getFunction(String funcName) {
        return function.get(funcName);
    }

    private List<Type> setList(Type... parameterTypes) {
        List<Type> list = new ArrayList<>();
        Collections.addAll(list, parameterTypes);
        return list;
    }
}
