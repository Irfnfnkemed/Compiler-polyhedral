package src.optimize.Inline.IRInline;

import src.IR.instruction.Block;
import src.Util.cell.Cell;
import src.Util.cell.ExtraBase;

import java.util.ArrayList;
import java.util.List;

public class ExtraInline extends ExtraBase {
    public List<Block> insertList;
    public Block block;
    public String toLabel, fromLabel;
    public Cell toCell = null;

    public ExtraInline(Block block_) {
        insertList = new ArrayList<>();
        block = block_;
    }
}
