package CoReMatch.pattern.cost.execution;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.pattern.cost.selection.CostEstimator;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.replay.ReplayPatternMatcher;
import CoReMatch.pattern.util.IntervalMergeCursor;
import CoReMatch.pattern.util.IntervalStatCursor;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class HistogramOnlyStrategy implements ExecutionStrategy {

    private final PrimaryIndex tree;
    private final TransformedPattern pattern;
    private final List<TimeInterval> replayIntervals;
    private final TransformedPattern.ExecutableConfiguration config;
    private final Estimates estimates;

    public HistogramOnlyStrategy(PrimaryIndex tree, TransformedPattern pattern, List<TimeInterval> replayIntervals,
                                 Estimates estimates) {
        this.tree = tree;
        this.pattern = pattern;
        this.replayIntervals = replayIntervals;
        this.estimates = estimates;
        pattern.disableAll();
        this.config = pattern.createExecution();
    }

    @Override
    public Estimates getEstimates() {
        return estimates;
    }

    @Override
    public ExecutionStrategy cloneWithNewEstimates(Estimates estimates) {
        return new HistogramOnlyStrategy(tree, pattern, replayIntervals, estimates);
    }

    @Override
    public String toString() {
        return String.format("Lightweight [%d regions], %s", replayIntervals.size(), estimates);
    }

    private Execution createIntervalCursor() {
        return CostEstimator.TRACE ? new TracingExecution() : new SilentExecution();
    }

    @Override
    public Cursor<Event> execute() throws TranslatorException, IncompatibleTypeException {
        var intervals = createIntervalCursor();

        ReplayPatternMatcher rpm = new ReplayPatternMatcher(tree, pattern.getDefinition());

        final Cursor<Event> c = rpm.executeMultiRegions(intervals.createReplayIntervals());

        if (CostEstimator.TRACE) {
            return new AbstractCursor<Event>() {

                @Override
                protected boolean hasNextObject() {
                    return c.hasNext();
                }

                @Override
                protected Event nextObject() {
                    return c.next();
                }

                @Override
                public void close() {
                    super.close();
                    c.close();
                    System.out.println(intervals.traceStats("    "));
                }
            };
        } else
            return c;
    }

    @Override
    public long executeIOOnly() {
        long numEvents = 0L;
        var exec = createIntervalCursor();
        var intervals = exec.createReplayIntervals();

        try {
            var ic = tree.query(intervals);
            try {
                ic.open();
                while (ic.hasNext()) {
                    var c = ic.next();
                    try {
                        c.open();
                        while (c.hasNext()) {
                            numEvents++;
                            c.next();
                        }
                    } finally {
                        c.close();
                    }
                }
            } finally {
                ic.close();
            }
        } finally {
            intervals.close();
        }
        CostEstimator.trace(exec.traceStats("    "));
        return numEvents;
    }

    @Override
    public boolean isEquivalent(TransformedPattern.ExecutableConfiguration config) {
        return getConfig().equals(config);
    }

    @Override
    public TransformedPattern.ExecutableConfiguration getConfig() {
        return this.config;
    }

    private static interface Execution {

        Cursor<TimeInterval> createReplayIntervals();

        String traceStats(String prefix);
    }

    private class TracingExecution implements Execution {
        IntervalStatCursor extendedStats = new IntervalStatCursor(Cursors.wrap(replayIntervals.iterator()));
        Cursor<TimeInterval> merged = new IntervalMergeCursor(extendedStats);
        IntervalStatCursor mergedStats = new IntervalStatCursor(merged);

        public Cursor<TimeInterval> createReplayIntervals() {
            return mergedStats;
        }

        @Override
        public String traceStats(String prefix) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.printf("%sReplay interval count             : %7d%n", prefix, extendedStats.getIntervalCount());
            pw.printf("%sAverage replay duration           : %7d%n", prefix, extendedStats.getAverageIntervalDuration());
            pw.printf("%sMerged interval count             : %7d%n", prefix, mergedStats.getIntervalCount());
            pw.printf("%sAverage merged interval duration  : %7d%n", prefix, mergedStats.getAverageIntervalDuration());
            pw.printf("%sTotal covered timespan            : %7d%n", prefix, mergedStats.getTotalDuration());
            pw.close();
            return sw.toString();
        }
    }

    private class SilentExecution implements Execution {
        Cursor<TimeInterval> merged = new IntervalMergeCursor<>(replayIntervals.iterator());

        public Cursor<TimeInterval> createReplayIntervals() {
            return merged;
        }

        @Override
        public String traceStats(String prefix) {
            return "";
        }
    }
}
