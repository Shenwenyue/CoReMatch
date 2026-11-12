package CoReMatch.esp.api.util;

import CoReMatch.esp.api.provider.EventSink;
import CoReMatch.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * A sink collecting all results in a list for later examination
 */
public class CollectingSink implements EventSink {

    /**
     * The list of events received
     */
    private final List<Event> events = new ArrayList<>();

    @Override
    public void receiveEvent(Event event) {
        events.add(event);
    }

    /**
     * @return The list of events received
     */
    public List<Event> getEvents() {
        return events;
    }

}
