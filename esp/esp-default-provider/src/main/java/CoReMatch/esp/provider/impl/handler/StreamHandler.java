package CoReMatch.esp.provider.impl.handler;

import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.Stream;
import CoReMatch.esp.api.provider.ExternalStreamHandle;
import CoReMatch.esp.api.provider.StreamHandle;
import CoReMatch.event.Event;
import CoReMatch.event.impl.SimpleEvent;

public class StreamHandler extends EPAHandler implements StreamHandle, ExternalStreamHandle {

    private final String name;

    public StreamHandler(String name, EventChannel channel) {
        super(new Stream(name));
        this.name = name;
        setResultChannel(channel);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void pushEvent(Object[] payload, long timestamp) {
        Object[] mapped = geoMapper.mapIncoming(payload);
        Event event = new SimpleEvent(mapped, timestamp, timestamp + 1);
        receive(event);
    }

    @Override
    public void pushEvent(Event event) {
        receive(event);
    }

    @Override
    public Stream getEPA() {
        return new Stream(this.name);
    }

}
