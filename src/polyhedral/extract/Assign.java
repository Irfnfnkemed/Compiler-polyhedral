package src.polyhedral.extract;

import java.util.ArrayList;
import java.util.List;

public class Assign {
    public MemVisit write;
    public List<MemVisit> read;
    public Coordinates coordinates;

    public Assign(Coordinates coordinates_) {
        read = new ArrayList<>();
        coordinates = new Coordinates(coordinates_);
    }

    public void setWrite(MemVisit write_) {
        write = write_;
    }

    public void setRead(MemVisit read_) {
        read.add(read_);
    }
}
