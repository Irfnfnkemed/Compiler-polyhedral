package src.IR.statement;

import java.util.HashMap;

public class ConstString extends IRStatement {
    public HashMap<String, Integer> constStringMap;//调用下标为i: @constString-i

    public ConstString() {
        constStringMap = new HashMap<>();
    }

    public String push(String content) {
        var id = constStringMap.computeIfAbsent(content, k -> constStringMap.size());
        return "@constString-" + id;
    }
}
