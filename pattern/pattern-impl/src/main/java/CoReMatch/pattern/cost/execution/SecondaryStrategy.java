package CoReMatch.pattern.cost.execution;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.pattern.cost.cursor.PatternAnchorCursor;
import CoReMatch.pattern.cost.cursor.PatternCandidateCursor;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.cost.selection.CostEstimator;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.replay.ReplayPatternMatcher;
import CoReMatch.pattern.util.IntervalIntersectionCursor;
import CoReMatch.pattern.util.IntervalMergeCursor;
import CoReMatch.pattern.util.IntervalStatCursor;
import CoReMatch.pattern.util.WindowExtendCursor;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import xxl.core.cursor.MinimalCursor;
import xxl.core.cursors.Cursor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SecondaryStrategy implements ExecutionStrategy {

    final PrimaryIndex tree;

    final List<? extends SecondaryTimeIndex<?>> secondaries;

    final TransformedPattern pattern;

    final List<TimeInterval> lightweightCandidates;

    final List<TimeInterval> extendedLightweightIntervals;
    final TransformedPattern.ExecutableConfiguration config;
    final Estimates estimates;

    final Map<String, CachedCursor> sharedQueryResultCache;

    private final ReplayIntervalPolicy replayIntervalPolicy;

    public SecondaryStrategy(PrimaryIndex tree, List<? extends SecondaryTimeIndex<?>> secondaries,
                             TransformedPattern pattern, List<TimeInterval> lightweightCandidates,
                             List<TimeInterval> extendedLightweightIntervals, TransformedPattern.ExecutableConfiguration config, Estimates estimates) {
        this(tree, secondaries, pattern, lightweightCandidates, extendedLightweightIntervals, config, estimates, null);
    }

    public SecondaryStrategy(PrimaryIndex tree, List<? extends SecondaryTimeIndex<?>> secondaries,
                             TransformedPattern pattern, List<TimeInterval> lightweightCandidates,
                             List<TimeInterval> extendedLightweightIntervals, TransformedPattern.ExecutableConfiguration config,
                             Estimates estimates, Map<String, CachedCursor> sharedQueryResultCache) {
        this(tree, secondaries, pattern, lightweightCandidates, extendedLightweightIntervals, config,
                estimates, sharedQueryResultCache, ReplayIntervalPolicy.fromSystemProperty());
    }

    public SecondaryStrategy(PrimaryIndex tree, List<? extends SecondaryTimeIndex<?>> secondaries,
                             TransformedPattern pattern, List<TimeInterval> lightweightCandidates,
                             List<TimeInterval> extendedLightweightIntervals, TransformedPattern.ExecutableConfiguration config,
                             Estimates estimates, Map<String, CachedCursor> sharedQueryResultCache,
                             ReplayIntervalPolicy replayIntervalPolicy) {
        this.tree = tree;
        this.secondaries = secondaries;
        this.pattern = pattern;
        this.lightweightCandidates = lightweightCandidates;
        this.extendedLightweightIntervals = extendedLightweightIntervals;
        this.config = config;
        this.estimates = estimates;
        this.sharedQueryResultCache = sharedQueryResultCache;
        ReplayIntervalPolicy requested = replayIntervalPolicy != null
                ? replayIntervalPolicy
                : ReplayIntervalPolicy.fromSystemProperty();
        this.replayIntervalPolicy = requested.resolve(config);
    }

    private static volatile boolean replayIntervalPolicyLogged;

    private void logReplayIntervalPolicyOnce() {
        if (replayIntervalPolicyLogged) {
            return;
        }
        synchronized (SecondaryStrategy.class) {
            if (replayIntervalPolicyLogged) {
                return;
            }
            if (replayIntervalPolicy == ReplayIntervalPolicy.RAW_CANDIDATE) {
                System.out.println(
                    "[SecondaryStrategy] replayIntervalPolicy=RAW_CANDIDATE "
                        + "(skip WindowExtendCursor and IntervalIntersectionCursor)");
            } else if (replayIntervalPolicy == ReplayIntervalPolicy.ANCHOR_ONLY) {
                System.out.println(
                    "[SecondaryStrategy] replayIntervalPolicy=ANCHOR_ONLY "
                        + "(skip replay pipeline and NFA; anchor TID point lookup)");
            } else {
                System.out.printf("[SecondaryStrategy] replayIntervalPolicy=%s%n", replayIntervalPolicy);
            }
            replayIntervalPolicyLogged = true;
        }
    }

    public SecondaryStrategy withReplayIntervalPolicy(ReplayIntervalPolicy policy) {
        return new SecondaryStrategy(tree, secondaries, pattern, lightweightCandidates, extendedLightweightIntervals,
                config, estimates, sharedQueryResultCache, policy);
    }

    public ReplayIntervalPolicy getReplayIntervalPolicy() {
        return replayIntervalPolicy;
    }

    @Override
    public Estimates getEstimates() {
        return estimates;
    }

    @Override
    public ExecutionStrategy cloneWithNewEstimates(Estimates estimates) {
        return new SecondaryStrategy(tree, secondaries, pattern, lightweightCandidates, extendedLightweightIntervals, config,
                estimates, sharedQueryResultCache, replayIntervalPolicy);
    }

    @Override
    public String toString() {
        return String.format("Secondary [%d sub-patterns, %d conditions], %s, details: %s", config.getSubPatterns().size(), config.getConditions().size(), estimates, config);
    }

    private PatternCandidateCursor createPatternCandidateCursor() {
        if (sharedQueryResultCache != null) {
            return new PatternCandidateCursor(pattern.getWindow(), config, secondaries, lightweightCandidates, sharedQueryResultCache);
        }
        return new PatternCandidateCursor(pattern.getWindow(), config, secondaries, lightweightCandidates);
    }

    private Execution createIntervalCursor() {
        logReplayIntervalPolicyOnce();
        if (CostEstimator.TRACE) {

            return new TracingExecution();
        } else {

            return new SilentExecution();
        }
    }

    public int getNumberOfIndexes() {
        return config.getConditions().size();
    }

    public Cursor<AnchorMatchResult> executeAnchorMatches() {
        logReplayIntervalPolicyOnce();
        PatternAnchorCursor anchorCursor = new PatternAnchorCursor(
                tree, pattern.getWindow(), config, secondaries, lightweightCandidates, sharedQueryResultCache,
                pattern.getAttributeEqualities());
        return anchorCursor;
    }

    @Override
    public Cursor<Event> execute() throws TranslatorException, IncompatibleTypeException {
        if (replayIntervalPolicy == ReplayIntervalPolicy.ANCHOR_ONLY) {
            return new AnchorMatchEventAdapter(executeAnchorMatches());
        }

        var exec = createIntervalCursor();

        ReplayPatternMatcher rpm = new ReplayPatternMatcher(tree, pattern.getDefinition());

        final Cursor<Event> c = rpm.executeMultiRegions(exec.createReplayIntervals());

        if (CostEstimator.TRACE) {
            return applyResidual(new MinimalCursor<>() {

                @Override
                public boolean hasNext() {
                    return c.hasNext();
                }

                @Override
                public Event next() {
                    return c.next();
                }

                @Override
                public void open() {
                    c.open();
                }

                @Override
                public void close() {
                    c.close();

                    System.out.println(exec.traceStats("    "));
                }
            });
        } else
            return applyResidual(c);

    }

    private Cursor<Event> applyResidual(Cursor<Event> cursor) {
        if (pattern.getAttributeEqualities().isEmpty()) {
            return cursor;
        }
        return new ResidualReplayCursor(cursor, pattern.getDefinition().getOutput(), pattern.getAttributeEqualities());
    }

    @Override
    public long executeIOOnly() {
        long numEvents = 0L;
        long t = -System.currentTimeMillis();
        var exec = createIntervalCursor();
        var intervals = exec.createReplayIntervals();

        try {

            intervals.open();
            t += System.currentTimeMillis();
            CostEstimator.trace("SIO took %d ms", t);

            t = -System.currentTimeMillis();
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
            t += System.currentTimeMillis();
            CostEstimator.trace("PIO took %d ms", t);
        } finally {
            intervals.close();
        }
        CostEstimator.trace(exec.traceStats("    "));

        return numEvents;
    }

    @Override
    public TransformedPattern.ExecutableConfiguration getConfig() {
        return this.config;
    }

    private static final class ReplayPipeline {
        final IntervalStatCursor candidateStats;
        final IntervalStatCursor afterExtendStats;
        final IntervalStatCursor mergedStats;
        final IntervalStatCursor intersectStats;
        final Cursor<TimeInterval> intersect;

        ReplayPipeline(IntervalStatCursor candidateStats, IntervalStatCursor afterExtendStats,
                       IntervalStatCursor mergedStats, IntervalStatCursor intersectStats,
                       Cursor<TimeInterval> intersect) {
            this.candidateStats = candidateStats;
            this.afterExtendStats = afterExtendStats;
            this.mergedStats = mergedStats;
            this.intersectStats = intersectStats;
            this.intersect = intersect;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ReplayPipeline buildReplayIntervalPipeline(boolean collectStats) {
        Cursor<? extends TimeInterval> candidates = createPatternCandidateCursor();
        IntervalStatCursor candidateStats = null;
        Cursor<? extends TimeInterval> candidateStage = candidates;
        if (collectStats) {
            candidateStats = new IntervalStatCursor(candidates);
            candidateStage = candidateStats;
        }

        Cursor<TimeInterval> afterExtend;
        if (replayIntervalPolicy == ReplayIntervalPolicy.EXTEND) {
            afterExtend = new WindowExtendCursor(candidateStage, config, pattern.getWindow());
        } else {
            afterExtend = (Cursor) candidateStage;
        }

        IntervalStatCursor afterExtendStats = null;
        Cursor<TimeInterval> extendStage = afterExtend;
        if (collectStats) {
            afterExtendStats = new IntervalStatCursor(afterExtend);
            extendStage = afterExtendStats;
        }

        Cursor<TimeInterval> merged = new IntervalMergeCursor(extendStage);
        IntervalStatCursor mergedStats = null;
        Cursor<TimeInterval> mergeStage = merged;
        if (collectStats) {
            mergedStats = new IntervalStatCursor(merged);
            mergeStage = mergedStats;
        }

        Cursor<TimeInterval> result;
        IntervalStatCursor intersectStats = null;
        if (replayIntervalPolicy == ReplayIntervalPolicy.EXTEND) {
            Cursor<TimeInterval> intersect = new IntervalIntersectionCursor(
                    mergeStage, extendedLightweightIntervals.iterator());
            result = intersect;
            if (collectStats) {
                intersectStats = new IntervalStatCursor(intersect);
                result = intersectStats;
            }
        } else {

            result = mergeStage;
        }

        return new ReplayPipeline(candidateStats, afterExtendStats, mergedStats, intersectStats, result);
    }

    private static interface Execution {

        Cursor<TimeInterval> createReplayIntervals();

        String traceStats(String prefix);
    }

    private class TracingExecution implements Execution {

        private final ReplayPipeline pipeline = buildReplayIntervalPipeline(true);

        public Cursor<TimeInterval> createReplayIntervals() {
            return pipeline.intersect;
        }

        @Override
        public String traceStats(String prefix) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.printf("%sReplay interval policy            : %s%n", prefix, replayIntervalPolicy);
            pw.printf("%sCandidate count                   : %8d%n", prefix, pipeline.candidateStats.getIntervalCount());
            pw.printf("%sAverage candidate duration        : %8d%n", prefix, pipeline.candidateStats.getAverageIntervalDuration());
            pw.printf("%sAverage replay duration           : %8d%n", prefix, pipeline.afterExtendStats.getAverageIntervalDuration());
            if (replayIntervalPolicy == ReplayIntervalPolicy.RAW_CANDIDATE) {
                pw.printf("%s(WindowExtend skipped; replay duration equals candidate)%n", prefix);
            }
            pw.printf("%sMerged interval count             : %8d%n", prefix, pipeline.mergedStats.getIntervalCount());
            pw.printf("%sAverage merged interval duration  : %8d%n", prefix, pipeline.mergedStats.getAverageIntervalDuration());
            pw.printf("%sMerged covered timespan           : %8d%n", prefix, pipeline.mergedStats.getTotalDuration());
            if (pipeline.intersectStats != null) {
                pw.printf("%sTrimmed interval count            : %8d%n", prefix, pipeline.intersectStats.getIntervalCount());
                pw.printf("%sAverage trimmed interval duration : %8d%n", prefix, pipeline.intersectStats.getAverageIntervalDuration());
                pw.printf("%sTrimmed covered timespan          : %8d%n", prefix, pipeline.intersectStats.getTotalDuration());
            } else {
                pw.printf("%sTrimmed intersect skipped (RAW_CANDIDATE; merged intervals used as-is)%n", prefix);
            }
            pw.close();
            return sw.toString();
        }
    }

    ;

    private class SilentExecution implements Execution {

        private final ReplayPipeline pipeline = buildReplayIntervalPipeline(false);

        public Cursor<TimeInterval> createReplayIntervals() {
            return pipeline.intersect;
        }

        @Override
        public String traceStats(String prefix) {
            return "";
        }
    }

    private static final class AnchorMatchEventAdapter implements MinimalCursor<Event> {

        private final Cursor<AnchorMatchResult> anchors;

        AnchorMatchEventAdapter(Cursor<AnchorMatchResult> anchors) {
            this.anchors = anchors;
        }

        @Override
        public void open() {
            anchors.open();
        }

        @Override
        public void close() {
            anchors.close();
        }

        @Override
        public boolean hasNext() {
            return anchors.hasNext();
        }

        @Override
        public Event next() {
            return anchors.next().getAnchors().get(0);
        }
    }

}
