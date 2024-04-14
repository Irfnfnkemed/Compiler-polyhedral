package src.optimize.ADCE;

import src.IR.instruction.Block;
import src.Util.cell.ExtraBase;

public class ExtraADCE extends ExtraBase {
    public boolean isActive = false;
    public boolean isVisited = false;
    public Block block;

    public ExtraADCE() {
        isActive = isVisited = false;
    }
}
