package CoReMatch.esp.bridges.nat.epa;

import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;

public interface NativeOperator {

    EventSchema getSchema();

    void process(EventChannel input, Event event);

    void setCallback(EPACallback callback);

    void flushState();
}
