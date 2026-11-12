package CoReMatch.pattern.cost.cursor;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;
import CoReMatch.pattern.cost.execution.AnchorMatchResult;
import CoReMatch.pattern.cost.execution.AttributeEquality;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.util.PhaseTimers;
import CoReMatch.pattern.util.PrimaryIndexEventLoader;
import xxl.core.cursors.AbstractCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Yields {@link AnchorMatchResult} groups: validated anchor combinations with full events from the primary index.
 * When attribute equalities are present, a group is returned only after those residual checks pass.
 */
public class PatternAnchorCursor extends AbstractCursor<AnchorMatchResult> {

    private final PatternCandidateBindingsIterator bindingsIterator;
    private final PrimaryIndex primaryIndex;
    private final List<AttributeEquality> equalities;
    private final int[] leftAnchor;
    private final int[] rightAnchor;
    private final int[] leftAttribute;
    private final int[] rightAttribute;
    private AnchorBinding nextBinding = null;
    private AnchorMatchResult pending;
    private boolean pendingReady;

    public PatternAnchorCursor(
            PrimaryIndex primaryIndex,
            long window,
            TransformedPattern.ExecutableConfiguration config,
            List<? extends SecondaryTimeIndex<?>> indexes,
            List<TimeInterval> filter,
            Map<String, CachedCursor> queryResultCache) {
        this(primaryIndex, window, config, indexes, filter, queryResultCache, List.of());
    }

    public PatternAnchorCursor(
            PrimaryIndex primaryIndex,
            long window,
            TransformedPattern.ExecutableConfiguration config,
            List<? extends SecondaryTimeIndex<?>> indexes,
            List<TimeInterval> filter,
            Map<String, CachedCursor> queryResultCache,
            List<AttributeEquality> equalities) {
        this.primaryIndex = primaryIndex;
        this.bindingsIterator = new PatternCandidateBindingsIterator(
                window, config, indexes, filter, queryResultCache);
        this.equalities = (equalities == null || equalities.isEmpty()) ? List.of() : List.copyOf(equalities);
        if (this.equalities.isEmpty()) {
            this.leftAnchor = null;
            this.rightAnchor = null;
            this.leftAttribute = null;
            this.rightAttribute = null;
        } else {
            Map<Character, Integer> symbolToAnchor = symbolIndexes(config);
            EventSchema schema = primaryIndex.getSchema();
            int n = this.equalities.size();
            this.leftAnchor = new int[n];
            this.rightAnchor = new int[n];
            this.leftAttribute = new int[n];
            this.rightAttribute = new int[n];
            for (int i = 0; i < n; i++) {
                AttributeEquality equality = this.equalities.get(i);
                this.leftAnchor[i] = anchorIndex(symbolToAnchor, equality.getLeftSymbol());
                this.rightAnchor[i] = anchorIndex(symbolToAnchor, equality.getRightSymbol());
                this.leftAttribute[i] = attributeIndex(schema, equality.getLeftAttribute());
                this.rightAttribute[i] = attributeIndex(schema, equality.getRightAttribute());
            }
        }
    }

    @Override
    public void open() {
        if (isOpened) {
            return;
        }
        super.open();
        bindingsIterator.open();
        if (equalities.isEmpty()) {
            nextBinding = bindingsIterator.computeNextBinding();
        } else {
            pendingReady = false;
        }
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
        if (equalities.isEmpty()) {
            return nextBinding != null;
        }
        if (!pendingReady) {
            pending = pullVerified();
            pendingReady = true;
        }
        return pending != null;
    }

    @Override
    protected AnchorMatchResult nextObject() {
        if (equalities.isEmpty()) {
            AnchorBinding binding = nextBinding;
            nextBinding = bindingsIterator.computeNextBinding();
            return materialize(binding);
        }
        AnchorMatchResult current = pending;
        pending = null;
        pendingReady = false;
        return current;
    }

    private AnchorMatchResult pullVerified() {
        AnchorBinding binding = bindingsIterator.computeNextBinding();
        while (binding != null) {
            AnchorMatchResult result = materialize(binding);
            if (accepts(result.getAnchors())) {
                return result;
            }
            binding = bindingsIterator.computeNextBinding();
        }
        return null;
    }

    private boolean accepts(List<Event> events) {
        for (int i = 0; i < equalities.size(); i++) {
            if (leftAnchor[i] < 0 || rightAnchor[i] < 0) {
                return false;
            }
            Object left = events.get(leftAnchor[i]).get(leftAttribute[i]);
            Object right = events.get(rightAnchor[i]).get(rightAttribute[i]);
            if (!Objects.equals(left, right)) {
                return false;
            }
        }
        return true;
    }

    private AnchorMatchResult materialize(AnchorBinding binding) {
        PhaseTimers.enterPhase3Materialize();
        try {
            List<Event> events = new ArrayList<>(binding.getAnchors().length);
            for (SubPatternCandidate anchor : binding.getAnchors()) {
                events.add(PrimaryIndexEventLoader.loadEvent(primaryIndex, anchor.getTid()));
            }
            return new AnchorMatchResult(events, binding.getInterval());
        } finally {
            PhaseTimers.exitPhase3Materialize();
        }
    }

    private static Map<Character, Integer> symbolIndexes(TransformedPattern.ExecutableConfiguration config) {
        Map<Character, Integer> symbolToAnchor = new HashMap<>();
        List<TransformedPattern.ExecutableSubPattern> subPatterns = config.getSubPatterns();
        for (int i = 0; i < subPatterns.size(); i++) {
            char symbol = subPatterns.get(i).getAnchorSymbol();
            if (symbol != 0) {
                symbolToAnchor.put(symbol, i);
            }
        }
        return symbolToAnchor;
    }

    private static int anchorIndex(Map<Character, Integer> symbolToAnchor, char symbol) {
        Integer index = symbolToAnchor.get(symbol);
        return index == null ? -1 : index;
    }

    private static int attributeIndex(EventSchema schema, String attribute) {
        try {
            return schema.getAttributeIndex(attribute);
        } catch (SchemaException e) {
            throw new IllegalArgumentException("Residual attribute is missing: " + attribute, e);
        }
    }
}
