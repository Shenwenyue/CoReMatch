package CoReMatch.db.core.secondaryindex;

import CoReMatch.db.event.TID;
import CoReMatch.event.Event;

import java.util.concurrent.Future;

public class EventAndTID {

    private Event event;
    private Future<TID> tid;

    public EventAndTID(Event event, Future<TID> tid) {
        this.event = event;
        this.tid = tid;
    }

    public Event getEvent() {
        return event;
    }

    public Future<TID> getTid() {
        return tid;
    }
}
