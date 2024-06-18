package src.polyhedral.dependency;

import src.polyhedral.extract.Affine;
import src.polyhedral.extract.Coordinates;

import java.util.List;

public class MemRW {
    public Coordinates coordinates;
    public List<Affine> addr;

    public MemRW(Coordinates coordinates_, List<Affine> addr_) {
        coordinates = coordinates_;
        addr = addr_;
    }
}
