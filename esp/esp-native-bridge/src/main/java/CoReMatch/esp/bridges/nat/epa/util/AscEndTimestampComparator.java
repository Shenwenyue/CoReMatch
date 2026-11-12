package CoReMatch.esp.bridges.nat.epa.util;

import CoReMatch.event.Event;

import java.util.Comparator;

/**
 * Simple comparator for timestamps on events
 */
public class AscEndTimestampComparator implements Comparator<Event> {

    @Override
    public int compare(Event e1, Event e2) {
        return Long.compare(e1.getT2(), e2.getT2());
    }

}
