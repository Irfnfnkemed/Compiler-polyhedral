package src.polyhedral.extract;

public class Index {
    public String varName;
    public long boundFrom, boundTo;
    public long step;

    public Index(String varName_, long boundFrom_, long boundTo_, long step_) {
        varName = varName_;
        boundFrom = boundFrom_;
        step = step_;
        if (step > 0) {
            boundTo = (boundTo_ - boundFrom_) / step_ * step_ + boundFrom_;
        } else {
            boundTo = (boundFrom_ - boundTo_) / (-step_) * step_ + boundFrom_;
        }
    }
}
