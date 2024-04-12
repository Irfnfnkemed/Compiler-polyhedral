package src.IR.instruction;

import src.IR.IRNode;

abstract public class Instruction extends IRNode {
    public boolean activeADCE = false;
    public boolean visitADCE = false;
    public String nowBlockADCE;
}
