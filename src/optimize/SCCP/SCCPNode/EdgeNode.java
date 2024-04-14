package src.optimize.SCCP.SCCPNode;

public class EdgeNode extends SCCPNode {
    public String fromBlock;
    public String toBlock;

    public EdgeNode(String fromBlock_, String toBlock_) {
        fromBlock = fromBlock_;
        toBlock = toBlock_;
    }
}
