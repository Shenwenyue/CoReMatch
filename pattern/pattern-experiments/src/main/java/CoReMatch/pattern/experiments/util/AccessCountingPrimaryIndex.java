package CoReMatch.pattern.experiments.util;

import CoReMatch.db.DBException;
import CoReMatch.db.core.IOMeasuring;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.db.core.primaryindex.impl.legacy.ImmutableParams;
import CoReMatch.db.core.primaryindex.impl.legacy.MutableParams;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.EventSchema;
import CoReMatch.event.TimeRepresentation;
import xxl.core.collections.containers.io.SuspendableContainer;
import xxl.core.cursor.DoubleCursor;
import xxl.core.cursor.MinimalCursor;
import xxl.core.cursors.Cursor;
import xxl.core.indexStructures.BPlusTree.Node;
import xxl.core.io.converters.Converter;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

public class AccessCountingPrimaryIndex extends PrimaryIndexImpl implements IOMeasuring {

    private PageAccessCountingContainer counter;

    private AtomicLong leafCounter;

    private AtomicLong intervalCounter = new AtomicLong();

    public AccessCountingPrimaryIndex(Path storageDirectory, EventSchema schema, TimeRepresentation timeRepresentation,
                                      ImmutableParams ip, MutableParams mp) throws DBException {
        super(storageDirectory, schema, timeRepresentation, ip, mp);
    }

    public AccessCountingPrimaryIndex(Path storageDirectory, MutableParams cfg) throws DBException {
        super(storageDirectory, cfg);
    }

    public AccessCountingPrimaryIndex(Path storageDirectory) throws DBException {
        super(storageDirectory);
    }

    @Override
    public void emptyBuffers() {
        getAppender().emptyTreeBuffer();
    }

    @Override
    public void resetMeasurement() {
        counter.resetCounters();
        intervalCounter.set(0L);
        leafCounter.set(0L);
    }

    public long getQueriedIntervals() {
        return intervalCounter.get();
    }

    @Override
    public DoubleCursor<PersistentEvent> query(long minKey, long maxKey) {
        intervalCounter.incrementAndGet();

        return super.query(minKey, maxKey);
    }

    @Override
    public Cursor<Cursor<PersistentEvent>> query(Cursor<TimeInterval> intervals) {

        final var src = super.query(intervals);

        return new MinimalCursor<>() {
            @Override
            public void open() {
                src.open();
            }
            @Override
            public void close() {
                src.close();
            }
            @Override
            public boolean hasNext() throws IllegalStateException {
                return src.hasNext();
            }
            @Override
            public Cursor<PersistentEvent> next() throws IllegalStateException, NoSuchElementException {
                intervalCounter.incrementAndGet();
                return src.next();
            }
        };
    }

    @Override
    public DoubleCursor<PersistentEvent> find(TID id) throws NoSuchEventException {
        intervalCounter.incrementAndGet();
        return super.find(id);
    }

    @Override
    public long getTotalReadBytes() {
        return (long) (counter.getReadCount() * getTree().BLOCK_SIZE);
    }

    @Override
    public long getTotalWrittenBytes() {
        return (long) (counter.getWriteCount() * getTree().BLOCK_SIZE);
    }

    public long getReadLeafBytes() {
        return getLeafPagesRead() * getTree().BLOCK_SIZE;
    }

    public long getReadInnerBytes() {
        return getInnerPagesRead() * getTree().BLOCK_SIZE;
    }

    public long getLeafPagesRead() {
        return leafCounter.get();
    }

    public long getInnerPagesRead() {
        return counter.readCount.get() - leafCounter.get();
    }

    public long getPagesRead() {
        return counter.readCount.get();
    }

    @Override
    protected SuspendableContainer createContainer(Path containerPath, Converter<?> treeNodeConverter) {
        leafCounter = new AtomicLong();

        counter = new PageAccessCountingContainer(super.createContainer(containerPath, treeNodeConverter)) {

            @Override
            public Object get(Object id, boolean unfix) throws NoSuchElementException {
                Node n = (Node) super.get(id, unfix);

                if (n.level == 0)
                    leafCounter.incrementAndGet();
                return n;
            }

            @Override
            public Object get(Object id) throws NoSuchElementException {
                Node n = (Node) super.get(id);

                if (n.level == 0)
                    leafCounter.incrementAndGet();
                return n;
            }
        };
        return counter;
    }

    @Override
    protected SuspendableContainer loadContainer(Path containerPath, Converter<?> treeNodeConverter) {
        leafCounter = new AtomicLong();
        counter = new PageAccessCountingContainer(super.loadContainer(containerPath, treeNodeConverter)) {

            @Override
            public Object get(Object id, boolean unfix) throws NoSuchElementException {
                Node n = (Node) super.get(id, unfix);

                if (n.level == 0)
                    leafCounter.incrementAndGet();
                return n;
            }

            @Override
            public Object get(Object id) throws NoSuchElementException {
                Node n = (Node) super.get(id);

                if (n.level == 0)
                    leafCounter.incrementAndGet();
                return n;
            }
        };
        return counter;
    }

}
