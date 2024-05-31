package src.polyhedral.dependency;


import java.util.ArrayList;
import java.util.List;

public class MemVisit {

    public String varName;
    public List<Affine> addr;

    public MemVisit() {
        addr = new ArrayList<>();
    }

    public MemVisit(MemVisit obj) {
        varName = obj.varName;
        addr = new ArrayList<>();
        for (Affine affine : obj.addr) {
            addr.add(new Affine(affine));
        }
    }

    public void setVarName(String varName_) {
        varName = varName_;
    }

    public void addDim(Affine affine) {
        addr.add(affine);
    }
}
