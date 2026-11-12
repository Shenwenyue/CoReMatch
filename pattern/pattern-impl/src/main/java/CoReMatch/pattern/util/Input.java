package CoReMatch.pattern.util;

import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.Stream;
import CoReMatch.esp.api.epa.UserDefinedEPA;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;

public class Input<T extends UserDefinedEPA> extends Stream implements EventChannel {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private final EventSchema schema;

    private final T def;

    public Input(EventSchema schema, T def) {
        super("ReplayIn");
        this.schema = schema;
        this.def = def;
    }

    @Override
    public EventSchema getSchema() {
        return schema;
    }

    @Override
    public void push(Event event) {
        this.def.process(this, event);
    }
}
