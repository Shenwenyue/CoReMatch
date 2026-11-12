package CoReMatch.pattern.cost.cursor;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.db.util.TimeInterval;
import xxl.core.cursors.AbstractCursor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yields replay time intervals {@code [t1,t2]} for validated anchor combinations.
 */
public class PatternCandidateCursor extends AbstractCursor<TimeInterval> {

    private final PatternCandidateBindingsIterator bindingsIterator;
    private TimeInterval nextResult = null;

    public PatternCandidateCursor(long window, TransformedPattern.ExecutableConfiguration config, List<? extends SecondaryTimeIndex<?>> indexes) {
        this(window, config, indexes, Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    public PatternCandidateCursor(long window, TransformedPattern.ExecutableConfiguration config, List<? extends SecondaryTimeIndex<?>> indexes, List<TimeInterval> filter) {
        this(window, config, indexes, filter, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public PatternCandidateCursor(long window, TransformedPattern.ExecutableConfiguration config, List<? extends SecondaryTimeIndex<?>> indexes, List<TimeInterval> filter, Map<String, CachedCursor> queryResultCache) {
        this.bindingsIterator = new PatternCandidateBindingsIterator(
                window, config, indexes, filter, queryResultCache);
    }

    @Override
    public void open() {
        if (isOpened) {
            return;
        }
        super.open();
        bindingsIterator.open();
        AnchorBinding binding = bindingsIterator.computeNextBinding();
        nextResult = binding != null ? binding.getInterval() : null;
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        super.close();
        bindingsIterator.close();
    }

    @Override
    protected boolean hasNextObject() {
        return nextResult != null;
    }

    @Override
    protected TimeInterval nextObject() {
        TimeInterval res = nextResult;
        AnchorBinding binding = bindingsIterator.computeNextBinding();
        nextResult = binding != null ? binding.getInterval() : null;
        return res;
    }
}
