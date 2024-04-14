package src.optimize.SCCP.SCCPNode;

public class SSANode extends SCCPNode {
    public String varName;

    public SSANode(String varName_) {
        varName = varName_;
    }
}
