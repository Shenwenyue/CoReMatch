package CoReMatch.db.core.primaryindex;

import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;
import xxl.core.cursor.DoubleCursor;
import xxl.core.cursors.Cursor;
import xxl.core.indexStructures.AggregateIndex.Aggregation;

import java.util.List;
import java.util.concurrent.Future;

public interface PrimaryIndex {

    TimeInterval getCoveredTimeInterval();

    int getMaxLeafEntries();

    int getMaxIndexEntries();

    long getNumberOfEvents();

    long getSerializedEventSize();

    long getContainerSize();

    int getHeight();

    long estimateIO(long numPages);

    EventSchema getSchema();

    Future<TID> insert(Event event);

    DoubleCursor<PersistentEvent> find(TID id) throws NoSuchEventException;

    DoubleCursor<PersistentEvent> query(long minKey, long maxKey);

    Cursor<Cursor<PersistentEvent>> query(Cursor<TimeInterval> intervals);

    Cursor<PersistentEvent> querySecondaryAttributes(long minKey, long maxKey, AttributeRange<?>... ranges) throws SchemaException;

    Aggregation getAggregates(String attribute, long minKey, long maxKey);

    void close();

    void flush();

    default PersistentEvent get(TID id) throws NoSuchEventException {
        try (DoubleCursor<PersistentEvent> res = find(id)) {
            res.open();
            return res.next();
        }
    }

    default DoubleCursor<PersistentEvent> query() {
        return query(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    default Cursor<PersistentEvent> querySecondaryAttributes(final AttributeRange<?>... ranges) throws SchemaException {
        return querySecondaryAttributes(Long.MIN_VALUE, Long.MAX_VALUE, ranges);
    }

    default Cursor<PersistentEvent> querySecondaryAttributes(final long minKey, final long maxKey,
                                                             final List<AttributeRange<?>> ranges) throws SchemaException {
        return querySecondaryAttributes(minKey, maxKey, ranges.toArray(new AttributeRange[ranges.size()]));
    }

    default Cursor<PersistentEvent> querySecondaryAttributes(final List<AttributeRange<?>> ranges) throws SchemaException {
        return querySecondaryAttributes(Long.MIN_VALUE, Long.MAX_VALUE, ranges);
    }

    default Number getAggregate(final String name, final String attribute) {
        return getAggregate(name, attribute, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    default Number getAggregate(final String name, final String attribute, final long minKey, final long maxKey) {

        final Number[] res = getAggregates(attribute, minKey, maxKey).getValues();
        switch (name.toLowerCase()) {
            case "sum":
                return res[0].doubleValue();
            case "min":
                return res[1].doubleValue();
            case "max":
                return res[2].doubleValue();
            case "count":
                return res[3].longValue();
            case "avg":
                return res[0].doubleValue() / res[3].doubleValue();
            default:
                throw new IllegalArgumentException("Unknown aggregate: " + name);
        }
    }

    default Aggregation getAggregates(final String attribute) {
        return getAggregates(attribute, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    default double getCompressionRatio() {
        long eventCount = getNumberOfEvents();
        long eventSize = getSerializedEventSize();
        long containerSize = getContainerSize();
        return (double) containerSize / (eventCount * eventSize);
    }

}
