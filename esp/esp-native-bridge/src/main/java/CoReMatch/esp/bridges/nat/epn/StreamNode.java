package CoReMatch.esp.bridges.nat.epn;

import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;

import java.util.ArrayList;
import java.util.List;

public class StreamNode implements EventChannel {

    private final List<OperatorNode> successors = new ArrayList<>();

    private final EventSchema schema;

    public StreamNode(EventSchema schema) {
        this.schema = schema;
    }

    public EventSchema getSchema() {
        return schema;
    }

    public void addSuccessor(OperatorNode node) {
        successors.add(node);
    }

    public boolean removeSuccessor(OperatorNode node) {
        return successors.remove(node);
    }

    public boolean hasSuccessor() {
        return !successors.isEmpty();
    }

    @Override
    public void push(Event event) {
        for (OperatorNode n : successors)
            n.process(this, event);
    }
}
