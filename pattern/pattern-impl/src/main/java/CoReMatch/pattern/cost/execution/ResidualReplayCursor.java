package CoReMatch.pattern.cost.execution;

import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.event.Event;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;

import java.util.List;
import java.util.Objects;

/**
 * Drops NFA output events that fail cross-event attribute equalities.
 * Output fields are the symbol bindings, for example {@code A_BEAT}.
 */
public final class ResidualReplayCursor extends AbstractCursor<Event> {

    private final Cursor<Event> source;
    private final int[] leftIndex;
    private final int[] rightIndex;
    private Event pending;

    public ResidualReplayCursor(Cursor<Event> source, Output output, List<AttributeEquality> equalities) {
        this.source = source;
        this.leftIndex = new int[equalities.size()];
        this.rightIndex = new int[equalities.size()];
        for (int i = 0; i < equalities.size(); i++) {
            AttributeEquality equality = equalities.get(i);
            leftIndex[i] = outputIndex(output, equality.leftOutputName());
            rightIndex[i] = outputIndex(output, equality.rightOutputName());
        }
    }

    @Override
    public void open() {
        if (isOpened) {
            return;
        }
        super.open();
        source.open();
        pending = pull();
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        super.close();
        source.close();
    }

    @Override
    protected boolean hasNextObject() {
        return pending != null;
    }

    @Override
    protected Event nextObject() {
        Event current = pending;
        pending = pull();
        return current;
    }

    private Event pull() {
        while (source.hasNext()) {
            Event event = source.next();
            if (accepts(event)) {
                return event;
            }
        }
        return null;
    }

    private boolean accepts(Event event) {
        for (int i = 0; i < leftIndex.length; i++) {
            if (!Objects.equals(event.get(leftIndex[i]), event.get(rightIndex[i]))) {
                return false;
            }
        }
        return true;
    }

    private static int outputIndex(Output output, String name) {
        int index = 0;
        for (String variable : output) {
            if (variable.equals(name)) {
                return index;
            }
            index++;
        }
        throw new IllegalArgumentException("Residual output field is missing: " + name);
    }
}
