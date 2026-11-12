package CoReMatch.pattern.util;

import CoReMatch.db.core.secondaryindex.EventID;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CachedCursor extends AbstractCursor<Pair<?, EventID>> {

    private final List<Pair<?, EventID>> cachedResults;
    private int currentIndex;
    private List<EventID> eventIdsDescending;

    public CachedCursor(Cursor<Pair<?, EventID>> originalCursor) {
        this.cachedResults = new ArrayList<>();

        originalCursor.open();
        while (originalCursor.hasNext()) {
            this.cachedResults.add(originalCursor.next());
        }
        originalCursor.close();
    }

    public CachedCursor(List<Pair<?, EventID>> pairs) {
        this.cachedResults = pairs;
    }

    private CachedCursor(List<Pair<?, EventID>> cachedResults, List<EventID> sharedEventIds) {
        this.cachedResults = cachedResults;
        this.eventIdsDescending = sharedEventIds;
    }

    public static CachedCursor sharedView(CachedCursor published) {
        return new CachedCursor(published.cachedResults, published.eventIdsDescending);
    }

    @Override
    public void open() {
        if (isOpened) return;
        super.open();
        this.currentIndex = 0;
    }

    @Override
    public void close() {
        if (isClosed) return;
        super.close();
    }

    @Override
    protected boolean hasNextObject() {
        return currentIndex < cachedResults.size();
    }

    @Override
    protected Pair<?, EventID> nextObject() {
        if (currentIndex < cachedResults.size()) {
            return cachedResults.get(currentIndex++);
        }
        return null;
    }

    public List<Pair<?, EventID>> getCachedResults() {
        return Collections.unmodifiableList(cachedResults);
    }

    public int size() {
        return cachedResults.size();
    }

    public synchronized List<EventID> getEventIdsDescending() {
        if (eventIdsDescending == null) {
            List<EventID> ids = new ArrayList<>(cachedResults.size());
            for (Pair<?, EventID> pair : cachedResults) {
                ids.add(pair.getElement2());
            }
            eventIdsDescending = Collections.unmodifiableList(ids);
        }
        return eventIdsDescending;
    }
}
