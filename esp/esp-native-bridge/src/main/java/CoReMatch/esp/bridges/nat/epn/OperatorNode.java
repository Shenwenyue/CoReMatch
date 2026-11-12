package CoReMatch.esp.bridges.nat.epn;

import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.event.Event;

public class OperatorNode extends StreamNode {

    private final NativeOperator operator;

    public OperatorNode(NativeOperator op) {
        super(op.getSchema());
        this.operator = op;
    }

    public void process(EventChannel input, Event event) {
        this.operator.process(input, event);
    }

    public NativeOperator getOperator() {
        return operator;
    }
}
