package CoReMatch.pattern.cost.selection;

import org.apache.commons.math3.analysis.integration.BaseAbstractUnivariateIntegrator;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.pattern.cost.estimation.prob.HypoExponentialDistribution;
import CoReMatch.pattern.cost.execution.Estimates;
import CoReMatch.pattern.cost.execution.HistogramOnlyStrategy;
import CoReMatch.pattern.cost.execution.SecondaryStrategy;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.cost.selection.PatternStats.ConditionStats;
import CoReMatch.pattern.cost.transform.SubPatternCondition;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.util.IntervalMergeCursor;
import CoReMatch.pattern.util.Util;
import CoReMatch.db.util.TimeInterval;
import xxl.core.cursors.Cursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CostEstimator {

    public static final boolean TRACE = false;

    private final PrimaryIndexImpl tree;

    private final List<? extends SecondaryTimeIndex<?>> secondaries;

    private final TransformedPattern pattern;

    private final List<SelSubPattern> selectivityEnrichedSubPatterns;

    private final double searchSpace;

    private final double extendedSearchSpace;

    private final double numEvents;

    private final double globalEventCount;

    private final List<TimeInterval> lightweightCandidates = new ArrayList<>();

    private final List<TimeInterval> extendedLightweightIntervals = new ArrayList<>();

    private final Map<SubPatternCondition.ConditionId, ConditionStats> statsMap = new HashMap<>();

    private final Map<String, CachedCursor> sharedQueryResultCache;

    private final UnivariateIntegrator integrator;

    public CostEstimator(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                         TransformedPattern pattern, PatternStats result) {
        this(primary, secondaries, pattern, result, null);
    }

    public CostEstimator(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                         TransformedPattern pattern, PatternStats result,
                         Map<String, CachedCursor> sharedQueryResultCache) {

        this.tree = primary;
        this.secondaries = secondaries;
        this.pattern = pattern;
        this.sharedQueryResultCache = sharedQueryResultCache;

        this.integrator = new RombergIntegrator(
                BaseAbstractUnivariateIntegrator.DEFAULT_RELATIVE_ACCURACY * 10000,
                BaseAbstractUnivariateIntegrator.DEFAULT_ABSOLUTE_ACCURACY,
                BaseAbstractUnivariateIntegrator.DEFAULT_MIN_ITERATIONS_COUNT,
                RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);

        var csi = result.getConditionStats().iterator();
        for (var c : pattern.collectConditions()) {
            statsMap.put(c.getId(), csi.next());

        }

        selectivityEnrichedSubPatterns =
                pattern.getSubPatterns().stream().map(sp -> new SelSubPattern(sp, statsMap)).collect(Collectors.toList());

        long duration = 0;
        long eventCount = 0;
        List<TimeInterval> tmpELI = new ArrayList<>();
        for (var mc : result.getMatchIntervals()) {
            lightweightCandidates.add(mc.getInterval());
            tmpELI.add(pattern.getLightweightTScope(mc));
            eventCount += mc.getEventCount();
            duration += mc.getInterval().getDuration();
        }

        Cursor<TimeInterval> intervals = new IntervalMergeCursor<>(tmpELI.iterator());
        intervals.open();
        long extendedDuration = 0L;
        while (intervals.hasNext()) {
            var i = intervals.next();
            extendedLightweightIntervals.add(i);
            extendedDuration += i.getDuration();
        }
        intervals.close();

        searchSpace = duration;
        extendedSearchSpace = extendedDuration;
        numEvents = eventCount;
        globalEventCount = tree.getNumberOfEvents();
    }

    private static long estimateCoveredTimespan(double l, double L, long unmergedIntervalCount) {
        double probOverlap = Math.min(l / L, 1.0);
        double pCovered = 1.0 - Math.pow((1.0 - probOverlap), unmergedIntervalCount);
        return (long) Math.ceil(L * pCovered);
    }

    private static long estimateMergedIntervalCount(double l, double L, long unmergedIntervalCount) {
        double probOverlap = Math.min(l / L, 1.0);
        double pUncovered = Math.pow((1.0 - Math.min(probOverlap, 1.0)), unmergedIntervalCount);
        return (long) Math.max(1.0, Math.ceil(unmergedIntervalCount * pUncovered));
    }

    public static void trace(String format, Object... args) {
        if (TRACE)
            System.out.printf(format, args);
    }

    public List<SelSubPattern> getSelectivityEnrichedSubPatterns() {
        return selectivityEnrichedSubPatterns;
    }

    public HistogramOnlyStrategy createLightweightStrategy() {
        return new HistogramOnlyStrategy(tree, pattern, extendedLightweightIntervals, estimateLightweightCosts());
    }

    public SecondaryStrategy createSecondaryStrategy(TransformedPattern.ExecutableConfiguration config) {
        return new SecondaryStrategy(tree, secondaries, pattern, lightweightCandidates, extendedLightweightIntervals, config,
                estimateSecondaryCosts(config), sharedQueryResultCache);
    }

    public Estimates estimateLightweightCosts() {
        long lightweightIO = estimatePrimaryIO((long) extendedSearchSpace, extendedLightweightIntervals.size());
        return new Estimates(
                lightweightIO,
                estimatePrimaryNavigationIO(extendedLightweightIntervals.size()),
                List.of(),
                extendedLightweightIntervals.size(),
                extendedLightweightIntervals.size(),
                extendedSearchSpace / tree.getCoveredTimeInterval().getDuration());
    }

    public Estimates estimateSecondaryCosts(TransformedPattern.ExecutableConfiguration config) {

        trace("Estimating config: %s%n", config);
        var sps = config.getSubPatterns().stream()
                .map(SubPatternStats::new)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        var espIter = sps.iterator();

        final SubPatternStats begin = espIter.next();
        final long initialIntervalCount = begin.matchCount;

        if (begin.matchCount == 0) {
            return new Estimates(0L, 0L, begin.secondaryCosts, 0, 0, 0.0);
        }

        List<Estimates.SecondaryIndexEstimate> secondaryCosts = sps.stream()
                .flatMap(x -> x.getIndexCosts().stream())
                .collect(Collectors.toCollection(ArrayList::new));

        SubPatternStats firstSP = begin.source.getIndex() == 0 ? begin : null;

        List<Double> lambdas = new ArrayList<>();

        while (espIter.hasNext()) {
            var sp = espIter.next();
            firstSP = sp.source.getIndex() == 0 ? sp : firstSP;
            lambdas.add(1.0 / sp.interArrivalTime);
        }

        RealDistribution dist;

        if (lambdas.size() > 1) {
            dist = new HypoExponentialDistribution(lambdas);
        } else if (lambdas.size() == 1) {
            dist = new ExponentialDistribution(1.0 / lambdas.get(0));
        } else {
            dist = new FakeDist();
        }

        double candidateInWindowProp = dist.cumulativeProbability(pattern.getWindow());
        double replayLength;

        if (candidateInWindowProp <= 0) {
            return new Estimates(0, 0, secondaryCosts, 0, 0, 0);
        }

        if (begin.source.getIndex() == 0 && config.isExact()) {
            replayLength = estimateAverageCandidateLength(config, dist, candidateInWindowProp);
        } else if (begin.source.getIndex() == 0) {
            replayLength = pattern.getWindow();
        }

        else if (firstSP != null) {
            double avgMatchLength = estimateAverageCandidateLength(config, dist, candidateInWindowProp);

            double remaining = pattern.getWindow() - avgMatchLength;
            int numInstances = (int) (remaining / firstSP.interArrivalTime);
            double durationAdd = numInstances * firstSP.interArrivalTime;

            replayLength = config.isExact() ? avgMatchLength : pattern.getWindow();
            replayLength += durationAdd;
        }

        else if (sps.size() == 1 && begin.source.getIndex() == pattern.getSubPatterns().size() - 1) {
            replayLength = pattern.getWindow();
        }

        else {
            replayLength = 2 * pattern.getWindow() - estimateAverageCandidateLength(config, dist, candidateInWindowProp);
        }

        long unmergedIntervalCount = (long) Math.ceil(candidateInWindowProp * initialIntervalCount);
        long primaryIO = 0L;
        long coveredTimespan = 0L;
        long mergedIntervalCount = 0L;

        {
            double fullDuration = tree.getCoveredTimeInterval().getDuration();
            for (TimeInterval ti : extendedLightweightIntervals) {
                double fraction = ti.getDuration() / fullDuration;

                long localUnmergedIntervalsCount = (long) Math.ceil(unmergedIntervalCount * fraction);
                long localMergedIntervalCount = estimateMergedIntervalCount(replayLength, ti.getDuration(), localUnmergedIntervalsCount);
                long localCoveredTimespan = estimateCoveredTimespan(replayLength, ti.getDuration(), localUnmergedIntervalsCount);

                long localPrimaryIO = estimatePrimaryIO(localCoveredTimespan, localMergedIntervalCount);

                mergedIntervalCount += localMergedIntervalCount;
                coveredTimespan += localCoveredTimespan;
                primaryIO += localPrimaryIO;
            }
        }

        long primaryNavigationIO = estimatePrimaryNavigationIO(mergedIntervalCount);
        double temporalCoverage = (double) coveredTimespan / tree.getCoveredTimeInterval().getDuration();
        return new Estimates(primaryIO, primaryNavigationIO, secondaryCosts, mergedIntervalCount, unmergedIntervalCount, temporalCoverage);
    }

    private long estimatePrimaryNavigationIO(long intervalCount) {
        int fanOut = tree.getMaxIndexEntries();
        long pages = (long) Math.ceil((double) tree.getNumberOfEvents() / tree.getMaxLeafEntries()) / fanOut;
        long innerCount = 0L;

        while (pages > 1) {
            innerCount += Math.min(intervalCount, pages);
            pages /= fanOut;
        }

        return tree.estimateIO(innerCount);
    }

    private long estimatePrimaryIO(long coveredTimespan, long mergedIntervalCount) {

        double totalLeafPages = Math.ceil((double) tree.getNumberOfEvents() / tree.getMaxLeafEntries());
        double totalCoveredTimespan = tree.getCoveredTimeInterval().getDuration();
        double timePerPage = totalCoveredTimespan / totalLeafPages;

        long timePerMergedInterval = (coveredTimespan + mergedIntervalCount - 1) / mergedIntervalCount;
        double pagesPerInterval = timePerMergedInterval / timePerPage + 1;

        double totalPages = pagesPerInterval * mergedIntervalCount;
        return tree.estimateIO((long) Math.ceil(totalPages));

    }

    private double estimateAverageCandidateLength(TransformedPattern.ExecutableConfiguration config, RealDistribution dist, double candidateInWindowProp) {
        double avgMatchLength;

        if (candidateInWindowProp >= 1.0) {
            avgMatchLength = dist.getNumericalMean();
        } else {
            avgMatchLength = integrator.integrate(5000, x -> dist.density(x) * x, 0, pattern.getWindow()) / candidateInWindowProp;
        }

        return avgMatchLength + config.getMinCandidateLength();
    }

    private static class FakeDist extends AbstractRealDistribution {

        @Override
        public double density(double x) {
            return 0;
        }

        @Override
        public double cumulativeProbability(double x) {
            return 1.0;
        }

        @Override
        public double getNumericalMean() {
            return 0;
        }

        @Override
        public double getNumericalVariance() {
            return 0;
        }

        @Override
        public double getSupportLowerBound() {
            return 0;
        }

        @Override
        public double getSupportUpperBound() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public boolean isSupportLowerBoundInclusive() {
            return true;
        }

        @Override
        public boolean isSupportUpperBoundInclusive() {
            return true;
        }

        @Override
        public boolean isSupportConnected() {
            return true;
        }
    }

    private class SubPatternStats implements Comparable<SubPatternStats> {

        final TransformedPattern.ExecutableSubPattern source;
        final long matchCount;
        final double interArrivalTime;
        final List<Estimates.SecondaryIndexEstimate> secondaryCosts;

        public SubPatternStats(TransformedPattern.ExecutableSubPattern esp) {
            this.secondaryCosts = new ArrayList<>();
            this.source = esp;
            double selectivity = 1.0;

            List<TransformedPattern.ExecutableCondition> conditions = esp.getConditions();

            Set<String> conditionAttributes = new HashSet<>();
            for (var c : conditions) {
                conditionAttributes.add(c.getRange().getName().toUpperCase());
            }

            Set<Integer> skipConditionIndices = new HashSet<>();

            Set<String> usedCompositeIndexes = new HashSet<>();

            for (int iCond = 0; iCond < conditions.size(); iCond++) {
                String condAttrName = conditions.get(iCond).getRange().getName().toUpperCase();

                for (var idx : secondaries) {
                    if (idx instanceof CompositeSecondaryTimeIndex) {
                        @SuppressWarnings("unchecked")
                        var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                        String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                        String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
                        String compositeKey = mainKey + "_" + secondKey;

                        if (condAttrName.equals(mainKey) && conditionAttributes.contains(secondKey)) {

                            if (!usedCompositeIndexes.contains(compositeKey)) {

                                for (int j = 0; j < conditions.size(); j++) {
                                    String attrName = conditions.get(j).getRange().getName().toUpperCase();
                                    if (attrName.equals(secondKey) && !skipConditionIndices.contains(j)) {
                                        skipConditionIndices.add(j);
                                    }
                                }
                                usedCompositeIndexes.add(compositeKey);
                            }
                        }
                    }
                }
            }

            int i = 0;
            while (i < conditions.size()) {
                var ec = conditions.get(i);
                var stats = statsMap.get(ec.getId());
                selectivity *= (stats.getSelectedEventCount() / numEvents);

                String attrName = ec.getRange().getName();

                if (skipConditionIndices.contains(i)) {
                    i++;
                    continue;
                }

                boolean foundIndex = false;

                for (var idx : secondaries) {
                    if (idx instanceof CompositeSecondaryTimeIndex) {
                        @SuppressWarnings("unchecked")
                        var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;

                        if (compositeIdx.getAttribute1Name().equalsIgnoreCase(attrName)) {

                            String secondKey = compositeIdx.getAttribute2Name().toUpperCase();

                            if (conditionAttributes.contains(secondKey)) {

                                long indexIO = (long) Math.ceil(compositeIdx.estimateIO(stats.getGlobalSelectivity()));
                                long indexResults = (long) Math.ceil(globalEventCount * stats.getGlobalSelectivity());
                                secondaryCosts.add(new Estimates.SecondaryIndexEstimate(indexIO, indexResults));
                                foundIndex = true;
                                break;
                            }
                        }
                    }
                }

                if (!foundIndex) {
                    for (var idx : secondaries) {
                        if (!(idx instanceof CompositeSecondaryTimeIndex) &&
                            idx.getAttributeName().equalsIgnoreCase(attrName)) {
                            long indexIO = (long) Math.ceil(idx.estimateIO(stats.getGlobalSelectivity()));
                            long indexResults = (long) Math.ceil(globalEventCount * stats.getGlobalSelectivity());
                            secondaryCosts.add(new Estimates.SecondaryIndexEstimate(indexIO, indexResults));
                            foundIndex = true;
                            break;
                        }
                    }
                }

                if (!foundIndex) {
                    for (var idx : secondaries) {
                        if (idx instanceof CompositeSecondaryTimeIndex) {
                            @SuppressWarnings("unchecked")
                            var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                            if (compositeIdx.getAttribute1Name().equalsIgnoreCase(attrName)) {
                                long indexIO = (long) Math.ceil(compositeIdx.estimateIO(stats.getGlobalSelectivity()));
                                long indexResults = (long) Math.ceil(globalEventCount * stats.getGlobalSelectivity());
                                secondaryCosts.add(new Estimates.SecondaryIndexEstimate(indexIO, indexResults));
                                foundIndex = true;
                                break;
                            }
                        }
                    }
                }

                if (!foundIndex) {

                }

                i++;
            }

            this.matchCount = (long) Math.ceil(selectivity * numEvents);
            this.interArrivalTime = searchSpace / matchCount;
        }

        public List<Estimates.SecondaryIndexEstimate> getIndexCosts() {
            return secondaryCosts;
        }

        @Override
        public int compareTo(SubPatternStats o) {
            return Long.compare(matchCount, o.matchCount);
        }
    }
}
