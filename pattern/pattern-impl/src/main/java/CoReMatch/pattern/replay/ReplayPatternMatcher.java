package CoReMatch.pattern.replay;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.esp.bridges.nat.epa.impl.pattern.NativePatternMatcher;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import CoReMatch.event.SchemaException;
import xxl.core.cursor.MinimalCursor;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.sources.EmptyCursor;

import java.util.NoSuchElementException;

public class ReplayPatternMatcher extends NativeReplayOperator<PatternMatcher> {

    public ReplayPatternMatcher(PrimaryIndex tree, PatternMatcher pm) throws TranslatorException, IncompatibleTypeException {
        super(tree, pm, new Fact());
    }

    public Cursor<Event> executeMultiRegions(final Cursor<TimeInterval> intervals) {

        final Cursor<Cursor<PersistentEvent>> regions = tree.query(intervals);

        return new MinimalCursor<Event>() {

            long intervalCount = 0L;

            Cursor<Event> currentResults = new EmptyCursor<>();

            Event next;

            @Override
            public void open() {
                regions.open();
                next = computeNext();
            }

            @Override
            public void close() {
                regions.close();
                currentResults.close();
                System.out.println("Intervals: " + intervalCount);
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
            private Event computeNext() {
                if (currentResults.hasNext())

                    return currentResults.next();
                while (!currentResults.hasNext() && regions.hasNext()) {
                    currentResults.close();
                    currentResults = executeDirect(regions.next());
                    intervalCount++;
                    currentResults.open();
                }

                return currentResults.hasNext() ? currentResults.next() : null;
            }
        };
    }

    static class Fact implements NativeOperatorFactory<PatternMatcher> {

        private NativePatternMatcher instance;

        @Override
        public NativeOperator create(PatternMatcher def, EventChannel... inputChannels) {
            if (instance == null) {
                try {
                    instance = new NativePatternMatcher(def, inputChannels[0]);
                } catch (SchemaException | IncompatibleTypeException | TranslatorException e) {
                    throw new RuntimeException(e);
                }
            }
            instance.clearState();
            return instance;
        }
    }

}
