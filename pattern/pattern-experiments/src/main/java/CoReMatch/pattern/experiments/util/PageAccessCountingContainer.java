package CoReMatch.pattern.experiments.util;

import xxl.core.collections.containers.Container;
import xxl.core.collections.containers.io.SuspendableContainer;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;
import xxl.core.functions.Function;
import xxl.core.io.converters.FixedSizeConverter;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

public class PageAccessCountingContainer implements SuspendableContainer {

    private final Container underlying;
    protected AtomicLong readCount;
    protected AtomicLong writeCount;

    public PageAccessCountingContainer(Container underlying) {
        this.underlying = underlying;
        this.readCount = new AtomicLong();
        this.writeCount = new AtomicLong();
    }

    public void resetCounters() {
        resetReadCount();
        resetWriteCount();
    }

    public void resetReadCount() {
        readCount.set(0);
    }

    public void resetWriteCount() {
        writeCount.set(0);
    }

    public long getReadCount() {
        return readCount.get();
    }

    public long getWriteCount() {
        return writeCount.get();
    }

    @Override
    public void clear() {
        underlying.clear();
    }

    @Override
    public void close() {
        underlying.close();
    }

    @Override
    public boolean contains(Object id) {
        return underlying.contains(id);
    }

    @Override
    public void flush() {
        underlying.flush();
    }

    @Override
    public void flush(Object id) {
        underlying.flush(id);
    }

    @Override
    public Object get(Object id, boolean unfix) throws NoSuchElementException {
        readCount.incrementAndGet();
        return underlying.get(id, unfix);
    }

    @Override
    public Object get(Object id) throws NoSuchElementException {
        readCount.incrementAndGet();
        return underlying.get(id);
    }

    @Override
    public Iterator getAll(Iterator ids, boolean unfix) throws NoSuchElementException {
        return underlying.getAll(new CountingIterator<>(ids, readCount), unfix);
    }

    @Override
    public Iterator getAll(Iterator ids) throws NoSuchElementException {
        return underlying.getAll(new CountingIterator<>(ids, readCount));
    }

    @Override
    public Iterator ids() {
        return underlying.ids();
    }

    @Override
    public Object insert(Object object, boolean unfix) {
        writeCount.incrementAndGet();
        return underlying.insert(object, unfix);
    }

    @Override
    public Object insert(Object object) {
        writeCount.incrementAndGet();
        return underlying.insert(object);
    }

    @Override
    public Iterator insertAll(Iterator objects, boolean unfix) {
        return underlying.insertAll(new CountingIterator<>(objects, writeCount), unfix);
    }

    @Override
    public Iterator insertAll(Iterator objects) {
        return underlying.insertAll(new CountingIterator<>(objects, writeCount));
    }

    @Override
    public boolean isUsed(Object id) {
        return underlying.isUsed(id);
    }

    @Override
    public Cursor objects() {
        return Cursors.wrap(new CountingIterator<>(underlying.objects(), writeCount));
    }

    @Override
    public FixedSizeConverter objectIdConverter() {
        return underlying.objectIdConverter();
    }

    @Override
    public int getIdSize() {
        return underlying.getIdSize();
    }

    @Override
    public void remove(Object id) throws NoSuchElementException {
        underlying.remove(id);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void removeAll(Iterator ids) throws NoSuchElementException {
        underlying.removeAll(ids);
    }

    @Override
    public Object reserve(Function getObject) {
        return underlying.reserve(getObject);
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @Override
    public void unfix(Object id) throws NoSuchElementException {
        underlying.unfix(id);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void unfixAll(Iterator ids) throws NoSuchElementException {
        underlying.unfixAll(ids);
    }

    @Override
    public void update(Object id, Object object, boolean unfix) throws NoSuchElementException {
        writeCount.incrementAndGet();
        underlying.update(id, object, unfix);
    }

    @Override
    public void update(Object id, Object object) throws NoSuchElementException {
        writeCount.incrementAndGet();
        underlying.update(id, object);
    }

    @Override
    public void updateAll(Iterator ids, Function function, boolean unfix) throws NoSuchElementException {
        underlying.updateAll(new CountingIterator<>(ids, writeCount), function, unfix);
    }

    @Override
    public void updateAll(Iterator ids, Function function) throws NoSuchElementException {
        underlying.updateAll(new CountingIterator<>(ids, writeCount), function);
    }

    @Override
    public void updateAll(Iterator ids, Iterator objects, boolean unfix) throws NoSuchElementException {
        underlying.updateAll(new CountingIterator<>(ids, writeCount), objects, unfix);
    }

    @Override
    public void updateAll(Iterator ids, Iterator objects) throws NoSuchElementException {
        underlying.updateAll(new CountingIterator<>(ids, writeCount), objects);
    }

    @Override
    public Object[] batchInsert(Object[] blocks) {
        writeCount.addAndGet(blocks.length);
        return underlying.batchInsert(blocks);
    }

    @Override
    public void suspend() {
        if (underlying instanceof SuspendableContainer)
            ((SuspendableContainer) underlying).suspend();
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");
    }

    @Override
    public void resume() {
        if (underlying instanceof SuspendableContainer)
            ((SuspendableContainer) underlying).resume();
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");
    }

    @Override
    public boolean isSuspended() {
        if (underlying instanceof SuspendableContainer)
            return ((SuspendableContainer) underlying).isSuspended();
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");
    }

    @Override
    public Iterator<?> idsBackwards() {
        if (underlying instanceof SuspendableContainer)
            return ((SuspendableContainer) underlying).idsBackwards();
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");
    }

    @Override
    public double getCompressionRatio() {
        if (underlying instanceof SuspendableContainer)
            return ((SuspendableContainer) underlying).getCompressionRatio();
        else
            return 1.0;
    }

    @Override
    public long getAllocatedSpace() {
        if (underlying instanceof SuspendableContainer)
            return ((SuspendableContainer) underlying).getAllocatedSpace();
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");
    }

    @Override
    public void allocateSpace(long space) {
        if (underlying instanceof SuspendableContainer)
            ((SuspendableContainer) underlying).allocateSpace(space);
        else
            throw new UnsupportedOperationException("Underlying container is not suspendable.");

    }

    private class CountingIterator<T> implements Iterator<T> {

        private final Iterator<T> underlying;

        private final AtomicLong counter;

        public CountingIterator(Iterator<T> underlying, AtomicLong counter) {
            this.underlying = underlying;
            this.counter = counter;
        }

        @Override
        public boolean hasNext() {
            return underlying.hasNext();
        }

        @Override
        public T next() {
            T result = underlying.next();
            counter.incrementAndGet();
            return result;
        }
    }

}
