package src.IR.instruction;

import src.Util.cell.Cell;
import src.Util.type.IRType;
import src.Util.type.Type;

public class Store extends Instruction {
    public IRType irType;
    public Cell value;
    public String toPointer;

    public Store(Store store) {
        irType = new IRType(store.irType);
        value = new Cell().set(store.value);
        toPointer = store.toPointer;
    }

    public Store(Type type_, Cell valueVar_, String toPointer_) {
        if (valueVar_.isConst) {
            init(new IRType(type_), valueVar_.varValue, toPointer_);
        } else {
            init(new IRType(type_), valueVar_.varName, toPointer_);
        }
    }

    public Store(IRType irType_, Cell valueVar_, String toPointer_) {
        if (valueVar_.isConst) {
            init(irType_, valueVar_.varValue, toPointer_);
        } else {
            init(irType_, valueVar_.varName, toPointer_);
        }
    }

    public Store(Type type_, long value_, String toPointer_) {
        init(new IRType(type_), value_, toPointer_);
    }

    public Store(Type type_, String valueVar_, String toPointer_) {
        init(new IRType(type_), valueVar_, toPointer_);
    }

    public Store(IRType irType_, long value_, String toPointer_) {
        init(irType_, value_, toPointer_);
    }

    public Store(IRType irType_, String valueVar_, String toPointer_) {
        init(irType_, valueVar_, toPointer_);
    }

    private void init(IRType irType_, String valueVar_, String toPointer_) {
        irType = irType_;
        value = new Cell().set(valueVar_);
        toPointer = toPointer_;
    }

    private void init(IRType irType_, long value_, String toPointer_) {
        irType = irType_;
        value = new Cell().set(value_);
        toPointer = toPointer_;
    }

}
