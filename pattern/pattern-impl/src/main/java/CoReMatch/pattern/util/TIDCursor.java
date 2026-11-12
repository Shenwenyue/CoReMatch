package CoReMatch.pattern.util;

import CoReMatch.db.core.secondaryindex.EventID;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import xxl.core.cursor.DoubleCursor;

import CoReMatch.event.SchemaException;
import CoReMatch.event.EventSchema;

public class TIDCursor extends AbstractCursor<EventID> {

    static final Random rand = new Random(1337L);

    final Cursor<Pair<?, EventID>> siQueryResult;

    Iterator<EventID> ids;
    private final boolean reverseOrder;

    public static void setPrimaryIndex(PrimaryIndex index) {
        FieldIndexManager.setPrimaryIndex(index);
    }

    public static int getAttributeIndex(String attributeName) {
        return FieldIndexManager.getAttributeIndex(attributeName);
    }

    public static boolean hasAttribute(String attributeName) {
        return FieldIndexManager.hasAttribute(attributeName);
    }

    public TIDCursor(Cursor<Pair<?, EventID>> siQueryResult) {
        this(siQueryResult, false);
    }

    public TIDCursor(Cursor<Pair<?, EventID>> siQueryResult, boolean reverseOrder) {
        this.siQueryResult = siQueryResult;
        this.reverseOrder = reverseOrder;
    }

;

    @Override
    public void open() {
        if (isOpened)
            return;

        super.open();

        List<EventID> tmp = new ArrayList<>();

        long time = -System.nanoTime();
        siQueryResult.open();
        siQueryResult.forEachRemaining(p -> tmp.add(p.getElement2()));
        siQueryResult.close();
        time += System.nanoTime();

        if (PMTiming.DO_TIMING) {
            PMTiming.getCurrentTiming().addSecondaryReadNanos(time);
        }

        if (reverseOrder) {
            Collections.reverse(tmp);
        }

        ids = tmp.iterator();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    protected boolean hasNextObject() {
        return ids.hasNext();
    }

    @Override
    protected EventID nextObject() {
        return ids.next();
    }

}
