package CoReMatch.pattern.util;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.queries.NoSuchEventException;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.event.Event;
import xxl.core.cursor.DoubleCursor;

/**
 * Loads a single full event from the primary index by {@link TID}.
 */
public final class PrimaryIndexEventLoader {

    private PrimaryIndexEventLoader() {
    }

    public static Event loadEvent(PrimaryIndex index, TID tid) {
        try (DoubleCursor<PersistentEvent> cursor = index.find(tid)) {
            cursor.open();
            if (!cursor.hasNext()) {
                throw new IllegalStateException("No event at TID " + tid);
            }
            return cursor.next();
        } catch (NoSuchEventException e) {
            throw new IllegalStateException("No event at TID " + tid, e);
        }
    }
}
