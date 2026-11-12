package CoReMatch.pattern.replay;

import CoReMatch.common.EPException;
import CoReMatch.pattern.util.Input;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.api.epa.UserDefinedEPA;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import xxl.core.cursors.Cursor;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;

public abstract class ESPReplayCursor<T extends UserDefinedEPA> implements Cursor<Event>, EPACallback {

    private final Input<T> in;
    T def;
    private Event next = null;
    private Queue<Event> resultBuffer = new ArrayDeque<>();
    private Cursor<PersistentEvent> inputQuery;
    private long processed;

    public ESPReplayCursor(EventSchema schemaIn, T def) {
        this.in = new Input<>(schemaIn, def);
        this.def = def;
        this.def.setCallback(this);
    }

    protected abstract Cursor<PersistentEvent> getInput(T epa);

    @Override
    public void open() {
        try {
            this.def.initialize(in);
            this.inputQuery = getInput(this.def);
            this.inputQuery.open();
            next = computeNext();
        } catch (EPException e) {
            throw new RuntimeException("Could not initialize ESPReplayCursor, input query failed.", e);
        }
    }

    @Override
    public void close() {
        try {
            this.def.destroy(false);
            this.inputQuery.close();
            this.def = null;
            this.inputQuery = null;
        } catch (EPException e) {
            throw new RuntimeException("Could not close ReplayOperator", e);
        }

    }

    private Event computeNext() {
        if (!resultBuffer.isEmpty())
            return resultBuffer.poll();

        while (resultBuffer.isEmpty() && inputQuery.hasNext()) {
            PersistentEvent pe = inputQuery.next();
            this.def.process(in, pe);
            processed++;
        }
        if (!resultBuffer.isEmpty())
            return resultBuffer.poll();

        return null;
    }

    @Override
    public boolean hasNext() throws IllegalStateException {
        return next != null;
    }

    @Override
    public Event next() throws IllegalStateException, NoSuchElementException {
        Event result = next;
        next = computeNext();
        return result;
    }

    @Override
    public Event peek() throws IllegalStateException, NoSuchElementException, UnsupportedOperationException {
        return next;
    }

    @Override
    public boolean supportsPeek() {
        return true;
    }

    @Override
    public void remove() throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Remove not supported by TPStream.");
    }

    @Override
    public boolean supportsRemove() {
        return false;
    }

    @Override
    public void update(Event object) throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Update not supported by TPStream.");

    }

    @Override
    public boolean supportsUpdate() {
        return false;
    }

    @Override
    public void reset() throws UnsupportedOperationException {
        close();
        open();
    }

    @Override
    public boolean supportsReset() {
        return true;
    }

    @Override
    public void receive(Event event) {
        this.resultBuffer.offer(event);
    }

}
