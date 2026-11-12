package CoReMatch.pattern.util;

import CoReMatch.db.core.secondaryindex.EventID;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.*;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import xxl.core.cursor.DoubleCursor;

import CoReMatch.event.SchemaException;
import CoReMatch.event.EventSchema;

public class ReverseOrderTIDCursor extends AbstractCursor<EventID> {

    static final Random rand = new Random(1337L);

    final Cursor<Pair<?, EventID>> siQueryResult;

    private final CachedCursor cachedSource;

    ListIterator<EventID> ids;
    List<EventID> tmpList;

    private final boolean needsSorting;

    public static void setPrimaryIndex(PrimaryIndex index) {
        FieldIndexManager.setPrimaryIndex(index);
    }

    public static int getAttributeIndex(String attributeName) {
        return FieldIndexManager.getAttributeIndex(attributeName);
    }

    public static boolean hasAttribute(String attributeName) {
        return FieldIndexManager.hasAttribute(attributeName);
    }

    public ReverseOrderTIDCursor(Cursor<Pair<?, EventID>> siQueryResult) {
        this(siQueryResult, true);
    }

    public ReverseOrderTIDCursor(Cursor<Pair<?, EventID>> siQueryResult, boolean needsSorting) {
        this.siQueryResult = siQueryResult;
        this.cachedSource = null;
        this.needsSorting = needsSorting;
    }

    public ReverseOrderTIDCursor(CachedCursor cached, boolean needsSorting) {
        this.siQueryResult = null;
        this.cachedSource = cached;
        this.needsSorting = needsSorting;
    }

    public ReverseOrderTIDCursor(List<EventID> eventIds) {
        this(eventIds, true);
    }

    public ReverseOrderTIDCursor(List<EventID> eventIds, boolean needsSorting) {
        this.siQueryResult = null;
        this.cachedSource = null;
        this.tmpList = eventIds;
        this.needsSorting = needsSorting;

    }

    @Override
    public void open() {
        if (isOpened)
            return;

        super.open();

        if (cachedSource != null) {
            tmpList = cachedSource.getEventIdsDescending();
            if (needsSorting) {
                tmpList = new ArrayList<>(tmpList);
                Collections.sort(tmpList, Collections.reverseOrder());
            }
            ids = tmpList.listIterator(tmpList.size());
            return;
        }

        if (siQueryResult == null && tmpList != null) {
            if (needsSorting) {
                Collections.sort(tmpList, Collections.reverseOrder());
            }
            ids = tmpList.listIterator(tmpList.size());
        } else {
            tmpList = new ArrayList<>();
            long time = -System.nanoTime();
            siQueryResult.open();
            while (siQueryResult.hasNext()) {
                Pair<?, EventID> p = siQueryResult.next();
                tmpList.add(p.getElement2());
            }
            siQueryResult.close();
            time += System.nanoTime();
            if (PMTiming.DO_TIMING) {
                PMTiming.getCurrentTiming().addSecondaryReadNanos(time);
            }
            if (needsSorting) {
                Collections.sort(tmpList, Collections.reverseOrder());
            }
            ids = tmpList.listIterator(tmpList.size());
        }
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected boolean hasNextObject() {

        return ids.hasPrevious();
    }

    @Override
    protected EventID nextObject() {

        return ids.previous();
    }

}
