package CoReMatch.esp.bridges.nat.epa.impl.window;

import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.window.TimeWindow;
import CoReMatch.event.Event;

/**
 * Implements a time-based window
 */
public class NativeTimeWindow extends NativeWindow {

    public NativeTimeWindow(EventChannel input, TimeWindow def) {
        super(input, def);
    }

    /**
     * @{inheritDoc
     */
    @Override
    public void process(EventChannel input, Event event) {
        long t1 = (event.getT1() / jump) * jump;
        long t2 = ((event.getT1() + size) / jump) * jump;
        if (t2 != t1)
            callback.receive(event.window(t1, t2));
    }

    @Override
    public void flushState() {
    }

}
