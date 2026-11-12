package CoReMatch.db.core.secondaryindex;

import CoReMatch.db.event.TID;
import CoReMatch.event.Event;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.Iterator;

public interface SecondaryTimeIndex<K extends Comparable<K>> {

    void build();

    void build(long startTime);

    void build(long startTime, long endTime);

    String getAttributeName();

    default Pair<K, EventID> exactMatchQueryID(K key) {
        Iterator<Pair<K, EventID>> ev = rangeQueryID(key, key);
        if (ev.hasNext())
            return ev.next();
        else
            return null;
    }

    Cursor<Pair<K, EventID>> rangeQueryID(K minKey, K maxKey);

    void insertEvent(Event event, TID tid, long sequenceId);

    void discardEvents(long timeStamp) throws UnsupportedOperationException;

    void destroy() throws UnsupportedOperationException;

    void close();

    double estimateIO(double selectivity);

}
