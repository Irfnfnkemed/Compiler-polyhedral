package src.polyhedral.dependency;

import src.polyhedral.extract.Domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Model {
    public HashMap<String, List<MemRW>> read;
    public HashMap<String, List<MemRW>> write;
    public List<Dependency> dependencies;

    public Model(Domain domain) {
        read = new HashMap<>();
        write = new HashMap<>();
        dependencies = new ArrayList<>();
        for (var assign : domain.stmtList) {
            if (!write.containsKey(assign.write.varName)) {
                write.put(assign.write.varName, new ArrayList<>());
            }
            write.get(assign.write.varName).add(new MemRW(assign.coordinates, assign.write.addr));
            for (var mem : assign.read) {
                if (!read.containsKey(mem.varName)) {
                    read.put(mem.varName, new ArrayList<>());
                }
                read.get(mem.varName).add(new MemRW(assign.coordinates, mem.addr));
            }
        }
    }

    public boolean setDependency() {
        for (var entry : write.entrySet()) {
            // write after write
            for (var memWriteFrom : entry.getValue()) {
                for (var memWriteTo : entry.getValue()) {
                    Dependency dependency = new Dependency(memWriteFrom, memWriteTo);
                    if (dependency.valid) {
                        dependencies.add(dependency);
                    } else {
                        return false;
                    }
                }
            }
            // write after read
            for (var memWriteTo : entry.getValue()) {
                for (var memReadFrom : read.get(entry.getKey())) {
                    Dependency dependency = new Dependency(memReadFrom, memWriteTo);
                    if (dependency.valid) {
                        dependencies.add(dependency);
                    } else {
                        return false;
                    }
                }
            }
            // read after write
            for (var memWriteFrom : entry.getValue()) {
                for (var memReadTo : read.get(entry.getKey())) {
                    Dependency dependency = new Dependency(memWriteFrom, memReadTo);
                    if (dependency.valid) {
                        dependencies.add(dependency);
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
