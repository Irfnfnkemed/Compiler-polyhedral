package src.polyhedral.extract;

public class Index {
    public String varName;
    public Affine boundFrom, boundTo;
    public long step;

    public Index() {
    }

    public void Simplify() {
        if (boundFrom.isConst() && boundTo.isConst()) {
            if (step > 0) {
                boundTo.bias = (boundTo.bias - boundFrom.bias) / step * step + boundFrom.bias;
            } else {
                boundTo.bias = (boundFrom.bias - boundTo.bias) / (-step) * step + boundFrom.bias;
            }
        }
    }
}
