package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;

public class Getelementptr extends Instruction {
    public String result;
    public IRType irType;
    public String from;
    public Cell index;
    public int offset = -1;

    public Getelementptr(Getelementptr getelementptr) {
        result = getelementptr.result;
        irType = new IRType(getelementptr.irType);
        from = getelementptr.from;
        index = new Cell().set(getelementptr.index);
        offset = getelementptr.offset;
    }

    public Getelementptr(String result_, IRType irType_, String from_, int offset_, String index_) {
        init(result_, irType_, from_, offset_);
        index = new Cell().set(index_);
    }

    public Getelementptr(String result_, IRType irType_, String from_, int offset_, long index_) {
        init(result_, irType_, from_, offset_);
        index = new Cell().set(index_);
    }

    public Getelementptr(String result_, IRType irType_, String from_, int offset_, Cell index_) {
        init(result_, irType_, from_, offset_);
        index = new Cell().set(index_);
    }

    private void init(String result_, IRType irType_, String from_, int offset_) {
        result = result_;
        irType = irType_;
        if (irType_.unitSize == 1) {
            irType.unitSize = 32;
            irType.unitName = "i32";
        }
        from = from_;
        offset = offset_;
    }
}
