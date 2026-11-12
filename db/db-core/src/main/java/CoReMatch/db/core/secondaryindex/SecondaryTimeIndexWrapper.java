package CoReMatch.db.core.secondaryindex;

import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.event.Event;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.indexStructures.BPlusLink;
import xxl.core.indexStructures.BPlusTree;
import xxl.core.util.Pair;

import java.util.*;

public class SecondaryTimeIndexWrapper<K extends Comparable<K>> {

    private final BPlusTree tree;

    private final SecondaryTimeIndex<K> index;

    public SecondaryTimeIndexWrapper(BPlusTree tree, SecondaryTimeIndex<K> index) {
        this.tree = tree;
        this.index = index;
    }

    /**
     * Executes an exact range query on this index structure and returns the
     * requested event. If there are multiple events with the given key, the
     * temporally first of them is returned. If there is no such event, null is
     * returned.
     *
     * @param key
     *            the key to look for
     * @return the event, if there is an event with the given key, null
     *         otherwise
     */
    PersistentEvent exactMatchQuery(K key) {
        Cursor<PersistentEvent> ev = rangeQuery(key, key);
        try {
            ev.open();
            if (ev.hasNext())
                return ev.next();
            else
                return null;
        } finally {
            ev.close();
        }
    }

    public Cursor<PersistentEvent> rangeQuery(K minKey, K maxKey) {
        return new SICursorOrdered(index.rangeQueryID(minKey, maxKey));
    }

    public Cursor<PersistentEvent> rangeQueryValueOrderd(K minKey, K maxKey) {
        return new SICursor(index.rangeQueryID(minKey, maxKey));
    }

    /*
     *
     * Inner cursors
     *
     */

    private class SICursorOrdered extends AbstractCursor<PersistentEvent> {

        private final Cursor<Pair<K, EventID>> indexCursor;

        private Iterator<EventID> ordered;

        public SICursorOrdered(Cursor<Pair<K, EventID>> indexCursor) {
            this.indexCursor = indexCursor;
        }

        @Override
        public void open() {
            if (isOpened)
                return;

            super.open();
            List<EventID> idList = new ArrayList<>();
            indexCursor.open();
            while (indexCursor.hasNext())
                idList.add(indexCursor.next().getElement2());
            indexCursor.close();
            Collections.sort(idList);
            this.ordered = idList.iterator();

        }

        @Override
        public boolean hasNextObject() throws IllegalStateException {
            return ordered.hasNext();
        }

        @Override
        public PersistentEvent nextObject() throws IllegalStateException, NoSuchElementException {
            EventID id = ordered.next();
            BPlusLink.Node node = (BPlusLink.Node) tree.container().get(id.getBlockId(), true);
            Event e = (Event) node.getEntry(id.getOffset());
            return new PersistentEvent(new TID(id.getBlockId(), id.getOffset()), e);
        }
    }

    private class SICursor extends AbstractCursor<PersistentEvent> {

        private final Cursor<Pair<K, EventID>> indexCursor;

        public SICursor(Cursor<Pair<K, EventID>> indexCursor) {
            this.indexCursor = indexCursor;
        }

        @Override
        public void open() {
            if (isOpened)
                return;
            super.open();
            indexCursor.open();
        }

        @Override
        public void close() {
            if (isClosed)
                return;
            super.close();
            indexCursor.close();
        }

        @Override
        public boolean hasNextObject() throws IllegalStateException {
            return indexCursor.hasNext();
        }

        @Override
        public PersistentEvent nextObject() throws IllegalStateException, NoSuchElementException {
            EventID id = indexCursor.next().getElement2();
            BPlusLink.Node node = (BPlusLink.Node) tree.container().get(id.getBlockId(), true);

            Event e = (Event) node.getEntry(id.getOffset());
            return new PersistentEvent(new TID(id.getBlockId(), id.getOffset()), e);
        }
    }
}
