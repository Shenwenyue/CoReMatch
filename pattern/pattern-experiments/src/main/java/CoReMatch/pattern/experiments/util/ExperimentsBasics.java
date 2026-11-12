package CoReMatch.pattern.experiments.util;

import CoReMatch.db.core.IOMeasuring;
import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.CompositeKey;
import CoReMatch.db.core.secondaryindex.EventID;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.CompositeAttributeRange;
import xxl.core.indexStructures.LogStructuredMergeBPlusTree;
import CoReMatch.pattern.cost.execution.Estimates;
import CoReMatch.pattern.cost.execution.ExecutionStrategy;
import CoReMatch.pattern.cost.selection.PatternStats;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.util.QueryResultCacheHelper;
import CoReMatch.pattern.util.QueryResultCacheKeys;
import CoReMatch.pattern.cost.transform.SubPatternCondition;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.experiments.ObjectSizeEstimator;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.epa.Stream;
import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.esp.api.epa.pattern.regex.Atom;
import CoReMatch.esp.api.epa.pattern.regex.KleeneStar;
import CoReMatch.esp.api.epa.pattern.regex.Pattern;
import CoReMatch.esp.api.epa.pattern.regex.Sequence;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.esp.api.expression.arithmetic.atomic.DoubleConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.Variable;
import CoReMatch.esp.api.expression.logical.True;
import CoReMatch.esp.api.expression.predicate.Between;
import xxl.core.collections.containers.Container;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExperimentsBasics {

    public static List<ExecutionStrategy> cloneWithGroundTruth(List<ExecutionStrategy> strategies, IOMeasures iom) {
        List<ExecutionStrategy> truth = new ArrayList<>();
        for (int i = 0; i < strategies.size(); i++) {
            var orig = strategies.get(i);
            truth.add(orig.cloneWithNewEstimates(iom.real.get(i)));
        }
        truth.sort(null);
        return truth;
    }
    public static final long consume(Cursor<?> c) {
        long results = 0L;
        System.out.println(c);

        c.open();
        while (c.hasNext()) {
            results++;
            c.next();
        }
        return results;
    }

    public static long warmUpPageCache(PrimaryIndexImpl primary) {
        Container con = primary.getTree().container();

        long pageCount = 0L;

        for (var i = con.getAll(con.ids(), true); i.hasNext(); ) {
            i.next();
            pageCount++;
        }
        return pageCount;
    }

    private static void logCompositeConditionQuery(Object primaryRange, Object secondaryRange,
                                                   double timeMs, double count, double selectivity) {
        System.out.printf(
            "Composite query [%s + %s] time: %.3f ms, count: %.0f, selectivity: %.6f%n",
            primaryRange, secondaryRange, timeMs, count, selectivity);
    }

    private static void logTypeConditionQuery(Object primaryRange, double timeMs, double count, double selectivity) {
        System.out.printf(
            "Type query [%s] time: %.3f ms, count: %.0f, selectivity: %.6f%n",
            primaryRange, timeMs, count, selectivity);
    }

    public static final PatternStats computePatternStats(AccessCountingPrimaryIndex primary,
                                                         List<? extends SecondaryTimeIndex<?>> secondaries, TransformedPattern tp) {
        return computePatternStats(primary, secondaries, tp, null);
    }

    public static final PatternStats computePatternStats(AccessCountingPrimaryIndex primary,
                                                         List<? extends SecondaryTimeIndex<?>> secondaries, TransformedPattern tp,
                                                         Map<String, CachedCursor> sharedQueryResultCache) {

        List<SubPatternCondition<?>> conditions = tp.collectConditions();

        final long totalCount = primary.getNumberOfEvents();
        final TimeInterval interval = primary.getCoveredTimeInterval();

        List<PatternStats.ConditionStats> stats = new ArrayList<>();

        Map<String, Double> conditionSelectivityCache = new HashMap<>();

        Map<String, CompositeSecondaryTimeIndex<?, ?>> secondaryKeyToCompositeIndex = new HashMap<>();

        Map<String, List<CompositeSecondaryTimeIndex<?, ?>>> primaryKeyToCompositeIndexes = new HashMap<>();
        for (var idx : secondaries) {
            if (idx instanceof CompositeSecondaryTimeIndex) {
                CompositeSecondaryTimeIndex<?, ?> compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
                secondaryKeyToCompositeIndex.put(secondKey, compositeIdx);
                primaryKeyToCompositeIndexes.computeIfAbsent(mainKey, k -> new ArrayList<>()).add(compositeIdx);
            }
        }

        Map<Integer, Map<String, AttributeRange<?>>> subPatternConditions = new HashMap<>();

        Map<Integer, Map<String, List<AttributeRange<?>>>> subPatternOrConditions = new HashMap<>();
        for (var cond : conditions) {
            int spIndex = cond.getId().subPatternIndex;
            String attrKey = cond.getRange().getName().toUpperCase();
            subPatternConditions.computeIfAbsent(spIndex, k -> new HashMap<>())
                .put(attrKey, cond.getRange());

            subPatternOrConditions.computeIfAbsent(spIndex, k -> new HashMap<>())
                .computeIfAbsent(attrKey, k -> new ArrayList<>())
                .add(cond.getRange());
        }

        java.util.Set<String> processedCompositeKeys = new java.util.HashSet<>();

        for (int i = 0; i < conditions.size(); i++) {

            AttributeRange<?> r = conditions.get(i).getRange();

            String attrName = r.getName().toUpperCase();
            int subPatternIndex = conditions.get(i).getId().subPatternIndex;

            String conditionKey = r.toString();

            double[] selectivity = {1.0};

            boolean conditionStatsDone = false;

            List<CompositeSecondaryTimeIndex<?, ?>> mainKeyComposites = primaryKeyToCompositeIndexes.get(attrName);
            if (mainKeyComposites != null && !mainKeyComposites.isEmpty()) {
                Map<String, AttributeRange<?>> spConds = subPatternConditions.get(subPatternIndex);
                List<CompositeSecondaryTimeIndex<?, ?>> applicableDimensions =
                    collectApplicableCompositeDimensionsForStats(mainKeyComposites, spConds);
                if (!applicableDimensions.isEmpty()) {
                    Map<String, List<AttributeRange<?>>> orCondsForSp = subPatternOrConditions.get(subPatternIndex);
                    List<AttributeRange<?>> orPrimaries = orCondsForSp != null ? orCondsForSp.get(attrName) : null;
                    boolean orSubpattern = orPrimaries != null && orPrimaries.size() > 1;

                    try {
                        if (applicableDimensions.size() >= 2) {
                            List<CachedCursor> branchCursors = new ArrayList<>();
                            List<AttributeRange<?>> secondaryRanges = new ArrayList<>();
                            for (CompositeSecondaryTimeIndex<?, ?> compositeIdx : applicableDimensions) {
                                String secondKeyName = compositeIdx.getAttribute2Name().toUpperCase();
                                AttributeRange<?> secondaryRange = spConds.get(secondKeyName);
                                secondaryRanges.add(secondaryRange);
                                CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(r, secondaryRange);
                                String queryCacheKey = compositeQueryCacheKey(compositeIdx, compositeRange);
                                String processedKey = compositeProcessedKey(
                                    subPatternIndex,
                                    compositeIdx.getAttribute1Name().toUpperCase(),
                                    secondKeyName);
                                boolean alreadyLogged = processedCompositeKeys.contains(queryCacheKey)
                                    || isSharedCacheHit(sharedQueryResultCache, queryCacheKey);
                                long branchStart = System.nanoTime();
                                double branchCount = queryCompositeMatchCount(
                                    compositeIdx, compositeRange, sharedQueryResultCache);
                                double branchMs = (System.nanoTime() - branchStart) / 1_000_000.0;
                                if (!alreadyLogged) {
                                    logCompositeConditionQuery(
                                        r, secondaryRange, branchMs, branchCount, branchCount / totalCount);
                                    processedCompositeKeys.add(queryCacheKey);
                                }
                                if (!orSubpattern) {
                                    processedCompositeKeys.add(processedKey);
                                }
                                if (sharedQueryResultCache != null) {
                                    CachedCursor branch = sharedQueryResultCache.get(queryCacheKey);
                                    if (branch != null) {
                                        branchCursors.add(branch);
                                    }
                                }
                                conditionSelectivityCache.put(
                                    selectivityCacheKey(subPatternIndex, queryCacheKey), branchCount / totalCount);
                            }
                            String conjKey = QueryResultCacheKeys.compositeConjunctionKey(
                                subPatternIndex, r, applicableDimensions, secondaryRanges);
                            CachedCursor conjCached;
                            if (sharedQueryResultCache != null) {
                                conjCached = sharedQueryResultCache.get(conjKey);
                                if (conjCached == null) {
                                    conjCached = QueryResultCacheHelper.buildConjunctionCursor(
                                        branchCursors, sharedQueryResultCache, conjKey);
                                }
                            } else {
                                conjCached = QueryResultCacheHelper.buildConjunctionCursor(
                                    branchCursors, new HashMap<>(), conjKey);
                            }
                            selectivity[0] = conjCached.size() / totalCount;
                            conditionSelectivityCache.put(
                                selectivityCacheKey(subPatternIndex, conjKey), selectivity[0]);
                        } else {
                            CompositeSecondaryTimeIndex<?, ?> mainKeyCompositeIdx = applicableDimensions.get(0);
                            String secondKeyName = mainKeyCompositeIdx.getAttribute2Name().toUpperCase();
                            AttributeRange<?> secondaryRange = spConds.get(secondKeyName);
                            CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(r, secondaryRange);
                            String queryCacheKey = compositeQueryCacheKey(mainKeyCompositeIdx, compositeRange);
                            String processedKey = compositeProcessedKey(
                                subPatternIndex,
                                mainKeyCompositeIdx.getAttribute1Name().toUpperCase(),
                                secondKeyName);

                            if (processedCompositeKeys.contains(processedKey)
                                    || processedCompositeKeys.contains(queryCacheKey)
                                    || (orSubpattern && isSharedCacheHit(sharedQueryResultCache, queryCacheKey))) {
                                double count = queryCompositeMatchCount(
                                    mainKeyCompositeIdx, compositeRange, sharedQueryResultCache);
                                selectivity[0] = count / totalCount;
                            } else {
                                long queryStartTime = System.nanoTime();
                                double count = queryCompositeMatchCount(
                                    mainKeyCompositeIdx, compositeRange, sharedQueryResultCache);
                                selectivity[0] = count / totalCount;
                                long queryEndTime = System.nanoTime();
                                double queryDurationMs = (queryEndTime - queryStartTime) / 1_000_000.0;
                                logCompositeConditionQuery(
                                    r, secondaryRange, queryDurationMs, count, selectivity[0]);
                                processedCompositeKeys.add(queryCacheKey);
                                if (!orSubpattern) {
                                    processedCompositeKeys.add(processedKey);
                                }
                            }
                            conditionSelectivityCache.put(
                                selectivityCacheKey(subPatternIndex, queryCacheKey), selectivity[0]);
                        }
                    } catch (Exception e) {
                        System.err.println("Composite primary-key query error: " + e.getMessage());
                        selectivity[0] = 1.0;
                    }
                    conditionStatsDone = true;
                }
            }

            CompositeSecondaryTimeIndex<?, ?> compositeIdx = secondaryKeyToCompositeIndex.get(attrName);
            String compositeCacheKey = null;

            if (compositeIdx != null) {
                String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                Map<String, AttributeRange<?>> spConds = subPatternConditions.get(subPatternIndex);
                AttributeRange<?> primaryRange = spConds != null ? spConds.get(mainKey) : null;
                if (primaryRange != null) {
                    CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(primaryRange, r);
                    String queryCacheKey = compositeQueryCacheKey(compositeIdx, compositeRange);
                    compositeCacheKey = selectivityCacheKey(subPatternIndex, queryCacheKey);
                }
            }

            String cacheKeyToCheck = (compositeCacheKey != null) ? compositeCacheKey : conditionKey;

            if (!conditionStatsDone && conditionSelectivityCache.containsKey(cacheKeyToCheck)) {

                selectivity[0] = conditionSelectivityCache.get(cacheKeyToCheck);
                conditionStatsDone = true;
            } else if (!conditionStatsDone) {

                if (compositeIdx != null) {

                    String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                    String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
                    String compositeKey = compositeProcessedKey(subPatternIndex, mainKey, secondKey);

                    if (processedCompositeKeys.contains(compositeKey)) {
                        String orUnionSelKey = selectivityCacheKey(subPatternIndex,
                            "OR_UNION_" + mainKey + "_" + r.toString());
                        if (conditionSelectivityCache.containsKey(orUnionSelKey)) {
                            selectivity[0] = conditionSelectivityCache.get(orUnionSelKey);
                        } else {
                            selectivity[0] = conditionSelectivityCache.getOrDefault(cacheKeyToCheck, 1.0);
                        }
                        conditionStatsDone = true;
                    } else {

                        Map<String, List<AttributeRange<?>>> orConds = subPatternOrConditions.get(subPatternIndex);
                        List<AttributeRange<?>> primaryRanges = (orConds != null) ? orConds.get(mainKey) : null;
                        boolean isOrCondition = primaryRanges != null && primaryRanges.size() > 1;

                        if (isOrCondition) {
                            processedCompositeKeys.add(compositeKey);
                            try {
                                List<CachedCursor> branchCursors = new ArrayList<>();
                                for (AttributeRange<?> primaryRange : primaryRanges) {
                                    CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                        primaryRange, r);
                                    String branchKey = compositeQueryCacheKey(compositeIdx, compositeRange);
                                    boolean alreadyLogged = processedCompositeKeys.contains(branchKey)
                                        || isSharedCacheHit(sharedQueryResultCache, branchKey);
                                    long branchStart = System.nanoTime();
                                    double branchCount = queryCompositeMatchCount(
                                        compositeIdx, compositeRange, sharedQueryResultCache);
                                    double branchMs = (System.nanoTime() - branchStart) / 1_000_000.0;
                                    if (!alreadyLogged) {
                                        logCompositeConditionQuery(
                                            primaryRange, r, branchMs, branchCount, branchCount / totalCount);
                                        processedCompositeKeys.add(branchKey);
                                    }
                                    if (sharedQueryResultCache != null) {
                                        CachedCursor branch = sharedQueryResultCache.get(branchKey);
                                        if (branch != null) {
                                            branchCursors.add(branch);
                                        }
                                    }
                                }
                                double totalUnionCount;
                                if (sharedQueryResultCache != null) {
                                    if (branchCursors.isEmpty()) {
                                        for (AttributeRange<?> primaryRange : primaryRanges) {
                                            CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                                primaryRange, r);
                                            CachedCursor branch = sharedQueryResultCache.get(
                                                compositeQueryCacheKey(compositeIdx, compositeRange));
                                            if (branch != null) {
                                                branchCursors.add(branch);
                                            }
                                        }
                                    }
                                    String unionKey = QueryResultCacheKeys.orUnionCompositeKey(
                                        subPatternIndex, compositeIdx, r);
                                    CachedCursor unionCached = sharedQueryResultCache.get(unionKey);
                                    if (unionCached == null) {
                                        unionCached = QueryResultCacheHelper.buildOrUnionCursor(
                                            branchCursors, sharedQueryResultCache, unionKey);
                                    }
                                    totalUnionCount = unionCached.size();
                                } else {
                                    java.util.Set<EventID> unionEventIDs = new java.util.HashSet<>();
                                    for (AttributeRange<?> primaryRange : primaryRanges) {
                                        CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                            primaryRange, r);
                                        java.lang.reflect.Method method = compositeIdx.getClass().getMethod(
                                            "rangeQueryID", Comparable.class, Comparable.class);
                                        @SuppressWarnings({"unchecked", "rawtypes"})
                                        Cursor<Pair<CompositeKey, EventID>> cursor =
                                            (Cursor<Pair<CompositeKey, EventID>>) method.invoke(
                                                compositeIdx, compositeRange.getLower(), compositeRange.getUpper());
                                        cursor.open();
                                        while (cursor.hasNext()) {
                                            Pair<CompositeKey, EventID> pair = cursor.next();
                                            unionEventIDs.add(pair.getElement2());
                                        }
                                    }
                                    totalUnionCount = unionEventIDs.size();
                                }
                                selectivity[0] = totalUnionCount / totalCount;

                                String orUnionSelKey = selectivityCacheKey(subPatternIndex,
                                    "OR_UNION_" + mainKey + "_" + r.toString());
                                conditionSelectivityCache.put(orUnionSelKey, selectivity[0]);
                            } catch (Exception e) {
                                System.err.println("Composite OR query error: " + e.getMessage());
                                e.printStackTrace();
                                selectivity[0] = 1.0;
                            }
                        } else {

                            Map<String, AttributeRange<?>> spConds = subPatternConditions.get(subPatternIndex);
                            AttributeRange<?> primaryRange = spConds != null ? spConds.get(mainKey) : null;

                            if (primaryRange != null) {

                                processedCompositeKeys.add(compositeKey);

                                long queryStartTime = System.nanoTime();
                                try {
                                    CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                        primaryRange, r);
                                    double count = queryCompositeMatchCount(
                                        compositeIdx, compositeRange, sharedQueryResultCache);
                                    selectivity[0] = count / totalCount;

                                    long queryEndTime = System.nanoTime();
                                    double queryDurationMs = (queryEndTime - queryStartTime) / 1_000_000.0;
                                    logCompositeConditionQuery(
                                        primaryRange, r, queryDurationMs, count, selectivity[0]);

                                    if (compositeCacheKey != null) {
                                        conditionSelectivityCache.put(compositeCacheKey, selectivity[0]);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Composite index query error: " + e.getMessage());
                                    selectivity[0] = 1.0;
                                }
                            } else {

                                double totalCount2 = 0.0;

                                try {

                                    java.lang.reflect.Method getLSMMethod = compositeIdx.getClass().getMethod("getLSMTree");
                                    @SuppressWarnings({"unchecked", "rawtypes"})
                                    LogStructuredMergeBPlusTree lsmTree =
                                        (LogStructuredMergeBPlusTree) getLSMMethod.invoke(compositeIdx);

                                    java.util.Iterator<?> iterator = lsmTree.query(null, null);
                                    while (iterator.hasNext()) {
                                        Pair<?, ?> pair = (Pair<?, ?>) iterator.next();
                                        Object keyObj = pair.getElement1();
                                        if (keyObj instanceof CompositeKey) {
                                            CompositeKey<?, ?> key = (CompositeKey<?, ?>) keyObj;
                                            Comparable<?> key2 = key.getKey2();
                                            if (key2 != null) {
                                                @SuppressWarnings("unchecked")
                                                Comparable<Object> comparableKey2 = (Comparable<Object>) key2;
                                                if (comparableKey2.compareTo(r.getLower()) >= 0 && comparableKey2.compareTo(r.getUpper()) <= 0) {
                                                    totalCount2 += 1;
                                                }
                                            }
                                        }
                                    }

                                    selectivity[0] = totalCount2 / totalCount;
                                    conditionSelectivityCache.put(conditionKey, selectivity[0]);
                                } catch (Exception e) {
                                    System.err.println("Composite secondary-key scan error: " + e.getMessage());
                                    e.printStackTrace();
                                    selectivity[0] = 1.0;
                                }
                            }
                        }
                    }
                } else {

                    boolean coveredByComposite = false;
                    CompositeSecondaryTimeIndex<?, ?> asSecondary = secondaryKeyToCompositeIndex.get(attrName);
                    if (asSecondary != null) {
                        String mainKey = asSecondary.getAttribute1Name().toUpperCase();
                        Map<String, AttributeRange<?>> spConds = subPatternConditions.get(subPatternIndex);
                        if (spConds != null && spConds.containsKey(mainKey)) {
                            coveredByComposite = true;
                        }
                    }
                    List<CompositeSecondaryTimeIndex<?, ?>> primComposites =
                        primaryKeyToCompositeIndexes.get(attrName);
                    if (primComposites != null) {
                        Map<String, AttributeRange<?>> spConds = subPatternConditions.get(subPatternIndex);
                        for (CompositeSecondaryTimeIndex<?, ?> pc : primComposites) {
                            String secondKey = pc.getAttribute2Name().toUpperCase();
                            if (spConds != null && spConds.containsKey(secondKey)) {
                                coveredByComposite = true;
                                break;
                            }
                        }
                    }

                    if (coveredByComposite) {
                        selectivity[0] = conditionSelectivityCache.getOrDefault(cacheKeyToCheck, 1.0);
                    } else {

                    secondaries.stream().filter(x -> !(x instanceof CompositeSecondaryTimeIndex) &&
                            x.getAttributeName().equalsIgnoreCase(r.getName())).findFirst()
                            .ifPresent(idx -> {
                                long queryStartTime = System.nanoTime();
                                double count;
                                if (sharedQueryResultCache != null) {
                                    boolean needsSorting = QueryResultCacheKeys.needsSorting(r);
                                    String cacheKey = QueryResultCacheKeys.independentKey(idx, r, needsSorting);
                                    Comparable lower = r.getLower();
                                    Comparable upper = r.getUpper();
                                    CachedCursor cached = QueryResultCacheHelper.getOrLoad(
                                        sharedQueryResultCache, cacheKey, () ->
                                            ((AccessCountingLSMTimeIndex) idx).rangeQueryID(lower, upper));
                                    count = QueryResultCacheHelper.countAll(cached);
                                } else {
                                    Comparable lower = r.getLower();
                                    Comparable upper = r.getUpper();
                                    var cursor = ((AccessCountingLSMTimeIndex) idx).rangeQueryID(lower, upper);
                                    cursor.open();
                                    count = 0.0;
                                    while (cursor.hasNext()) {
                                        cursor.next();
                                        count += 1;
                                    }
                                }
                                selectivity[0] = count / totalCount;
                                double queryDurationMs = (System.nanoTime() - queryStartTime) / 1_000_000.0;
                                logTypeConditionQuery(r, queryDurationMs, count, selectivity[0]);
                            });
                    conditionSelectivityCache.put(conditionKey, selectivity[0]);
                    }
                }
            }

            final SubPatternCondition.ConditionId id = conditions.get(i).getId();

            stats.add(new PatternStats.ConditionStats() {

                @Override
                public Iterator<TimeInterval> intervals() {
                    return List.of(interval).iterator();
                }

                @Override
                public SubPatternCondition.ConditionId getConditionId() {
                    return id;
                }

                @Override
                public long getTotalEventCount() {
                    return totalCount;
                }

                @Override
                public long getTotalDuration() {
                    return interval.getDuration();
                }

                @Override
                public double getSelectedEventCount() {
                    return totalCount * selectivity[0];
                }

                @Override
                public int getNumIntervals() {
                    return 1;
                }

                @Override
                public double getLocalSelectivity() {
                    return selectivity[0];
                }

                @Override
                public double getGlobalSelectivity() {
                    return selectivity[0];
                }
            });
        }

        return new PatternStats() {

            @Override
            public List<MatchInterval> getMatchIntervals() {
                return List.of(new MatchInterval() {

                    @Override
                    public TimeInterval getInterval() {
                        return interval;
                    }

                    @Override
                    public long getEventCount() {
                        return totalCount;
                    }

                    @Override
                    public long getExtendEnd() {
                        return interval.getT1();
                    }

                    @Override
                    public long getExtendBegin() {
                        return interval.getT2();
                    }

                });
            }

            @Override
            public List<ConditionStats> getConditionStats() {
                return stats;

            }

        };
    }

    private static String selectivityCacheKey(int subPatternIndex, String queryCacheKey) {
        return "SP" + subPatternIndex + "|" + queryCacheKey;
    }

    private static String compositeQueryCacheKey(CompositeSecondaryTimeIndex<?, ?> compositeIdx,
                                                 CompositeAttributeRange<?, ?> compositeRange) {
        boolean needsSorting = QueryResultCacheKeys.needsSorting(compositeRange);
        return QueryResultCacheKeys.compositeKey(compositeIdx, compositeRange, needsSorting);
    }

    private static String compositeProcessedKey(int subPatternIndex, String mainKey, String secondKey) {
        return subPatternIndex + "_" + mainKey + "_" + secondKey;
    }

    private static List<CompositeSecondaryTimeIndex<?, ?>> collectApplicableCompositeDimensionsForStats(
            List<CompositeSecondaryTimeIndex<?, ?>> compositesForMainKey,
            Map<String, AttributeRange<?>> spConds) {
        List<CompositeSecondaryTimeIndex<?, ?>> applicable = new ArrayList<>();
        if (spConds == null) {
            return applicable;
        }
        for (CompositeSecondaryTimeIndex<?, ?> compositeIdx : compositesForMainKey) {
            String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
            if (spConds.containsKey(secondKey)) {
                applicable.add(compositeIdx);
            }
        }
        applicable.sort(Comparator.comparing(CompositeSecondaryTimeIndex::getAttribute2Name));
        return applicable;
    }

    private static boolean isSharedCacheHit(Map<String, CachedCursor> sharedQueryResultCache, String cacheKey) {
        return sharedQueryResultCache != null && sharedQueryResultCache.containsKey(cacheKey);
    }

    private static double queryCompositeMatchCount(
            CompositeSecondaryTimeIndex<?, ?> compositeIdx,
            CompositeAttributeRange<?, ?> compositeRange,
            Map<String, CachedCursor> sharedQueryResultCache) throws Exception {
        String cacheKey = compositeQueryCacheKey(compositeIdx, compositeRange);
        if (sharedQueryResultCache != null) {
            final CompositeSecondaryTimeIndex<?, ?> idxRef = compositeIdx;
            final CompositeAttributeRange<?, ?> rangeRef = compositeRange;
            CachedCursor cached = QueryResultCacheHelper.getOrLoad(sharedQueryResultCache, cacheKey, () -> {
                try {
                    java.lang.reflect.Method method = idxRef.getClass().getMethod(
                        "rangeQueryID", Comparable.class, Comparable.class);
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Cursor<Pair<?, EventID>> cursor = (Cursor<Pair<?, EventID>>) method.invoke(
                        idxRef, rangeRef.getLower(), rangeRef.getUpper());
                    return cursor;
                } catch (Exception e) {
                    return new xxl.core.cursors.sources.EmptyCursor<>();
                }
            });
            return QueryResultCacheHelper.countAll(cached);
        }
        java.lang.reflect.Method method = compositeIdx.getClass().getMethod(
            "rangeQueryID", Comparable.class, Comparable.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Cursor<Pair<CompositeKey, EventID>> cursor = (Cursor<Pair<CompositeKey, EventID>>) method.invoke(
            compositeIdx, compositeRange.getLower(), compositeRange.getUpper());
        cursor.open();
        double count = 0.0;
        while (cursor.hasNext()) {
            cursor.next();
            count += 1;
        }
        cursor.close();
        return count;
    }

    public static PatternStats toUniformAssumption(AccessCountingPrimaryIndex primary, PatternStats hResult) {
        long totalEvents = primary.getNumberOfEvents();
        TimeInterval fullInterval = primary.getCoveredTimeInterval();

        List<PatternStats.ConditionStats> css = new ArrayList<>();
        for (PatternStats.ConditionStats cs : hResult.getConditionStats()) {
            css.add(new PatternStats.ConditionStats() {

                @Override
                public SubPatternCondition.ConditionId getConditionId() {
                    return cs.getConditionId();
                }

                @Override
                public long getTotalEventCount() {
                    return totalEvents;
                }

                @Override
                public long getTotalDuration() {
                    return fullInterval.getDuration();
                }

                @Override
                public int getNumIntervals() {
                    return 1;
                }

                @Override
                public double getSelectedEventCount() {
                    return totalEvents * cs.getGlobalSelectivity();
                }

                @Override
                public Iterator<TimeInterval> intervals() {
                    return List.of(fullInterval).iterator();
                }

                @Override
                public double getGlobalSelectivity() {
                    return cs.getGlobalSelectivity();
                }

                @Override
                public double getLocalSelectivity() {
                    return cs.getGlobalSelectivity();
                }
            });
        }

        return new PatternStats() {

            @Override
            public List<MatchInterval> getMatchIntervals() {
                return List.of(new MatchInterval() {
                    @Override
                    public TimeInterval getInterval() {
                        return fullInterval;
                    }

                    @Override
                    public long getExtendEnd() {
                        return fullInterval.getT2();
                    }

                    @Override
                    public long getExtendBegin() {
                        return fullInterval.getT1();
                    }

                    @Override
                    public long getEventCount() {
                        return totalEvents;
                    }
                });
            }

            @Override
            public List<ConditionStats> getConditionStats() {
                return css;
            }
        };
    }

    public static PatternMatcher randomPattern(Random rand, double maxSelectivity, long window, final int numSymbols) {
        return randomPattern(rand, maxSelectivity, window, numSymbols, "A4");
    }

    public static PatternMatcher randomPattern(Random rand, double maxSelectivity, long window, final int numSymbols,
                                               String firstAttrib) {

        final String varOut = "OUT";

        int remaining = numSymbols;
        char[] names = new char[51];
        for (char c = 'A'; c <= 'Z'; c++) {
            names[c - 'A'] = c;
        }
        for (char c = 'a'; c < 'z'; c++) {
            names[c - 'a' + 26] = c;
        }

        int next = 0;
        char skip = 'z';

        List<Symbol> symbols = new ArrayList<>();
        List<Pattern> pattern = new ArrayList<>();

        symbols.add(new Symbol(skip, new True()));

        while (remaining > 0) {
            int take = rand.nextInt(remaining) + 1;
            remaining -= take;

            for (int i = 0; i < take; i++) {
                String attr = (next == 0) ? firstAttrib : "A4";

                Bindings bs = (next == 0) ? new Bindings(new Binding(varOut, new Variable(attr))) : new Bindings();

                char name = names[next++];
                double selectivity = rand.nextDouble() * maxSelectivity;
                double begin = 1.0;
                while (begin + selectivity > 1.0)
                    begin = rand.nextDouble();

                symbols.add(new Symbol(name,
                        new Between(new Variable(attr), new DoubleConstant(begin), new DoubleConstant(begin + selectivity)),
                        bs));
                pattern.add(new Atom(name));
            }
            if (remaining > 0)
                pattern.add(new KleeneStar(new Atom(skip)));
        }

        return new PatternMatcher(new Stream("dummy"), window,
                new Symbols(symbols),
                new Sequence(pattern),
                new Output(varOut));
    }

    public static class IOMeasures {
        private List<Estimates> estimated = new ArrayList<>();

        private List<Estimates> real = new ArrayList<>();

        private List<Long> realSIO = new ArrayList<>();

        public List<Estimates> getEstimated() {
            return estimated;
        }

        public List<Estimates> getReal() {
            return real;
        }

        public List<Long> getRealSIO() {
            return realSIO;
        }

        public void updateAndReset(ExecutionStrategy s, AccessCountingPrimaryIndex primary,
                                   List<? extends SecondaryTimeIndex<?>> secondaries) {
            estimated.add(s.getEstimates());

            Estimates actual = new Estimates(
                    primary.getReadLeafBytes(),
                    primary.getReadInnerBytes(),

                    s.getEstimates().getSecondary(),

                    primary.getQueriedIntervals(),
                    -1L,
                    0.0
            );

            real.add(actual);

            long totalSecondaryIO = 0L;
            for (var si : secondaries) {
                if (si instanceof IOMeasuring) {
                    totalSecondaryIO += ((IOMeasuring) si).getTotalReadBytes();
                }
            }
            realSIO.add(totalSecondaryIO);

            resetMeasures(primary, secondaries);
        }

        public void duplicateLast() {
            estimated.add(estimated.get(estimated.size() - 1));
            real.add(real.get(real.size() - 1));
        }

        public void resetMeasures(AccessCountingPrimaryIndex primary,
                                  List<? extends SecondaryTimeIndex<?>> secondaries) {
            primary.emptyBuffers();
            primary.resetMeasurement();

            for (var si : secondaries) {
                if (si instanceof IOMeasuring) {
                    IOMeasuring measuring = (IOMeasuring) si;
                    measuring.resetMeasurement();
                    measuring.emptyBuffers();
                }
            }
        }

        public String toString() {

            var pioErrors = calcErrors(x -> x.getPrimaryLeafIO() + x.getPrimaryNavigationIO());
            var pioLeafErrors = calcErrors(Estimates::getPrimaryLeafIO);
            var pioInnerErrors = calcErrors(Estimates::getPrimaryNavigationIO);
            var sioErrors = calcSIOErrors();

            double errAccumPIO = pioErrors.stream().collect(Collectors.averagingDouble(x -> x));
            double errAccumPIOLeaf = pioLeafErrors.stream().collect(Collectors.averagingDouble(x -> x));
            double errAccumPIOInner = pioInnerErrors.stream().collect(Collectors.averagingDouble(x -> x));
            double errAccumSIO = sioErrors.stream().collect(Collectors.averagingDouble(x -> x));
            StringBuilder result = new StringBuilder();
            result.append("IOMeasurements [").append(String.format("%n"));
            result.append("  Expected PIO          : ").append(estimated.stream().map(x -> x.getPrimaryLeafIO() + x.getPrimaryNavigationIO()).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual PIO            : ").append(real.stream().map(x -> x.getPrimaryLeafIO() + x.getPrimaryNavigationIO()).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected PIO (Leaves) : ").append(estimated.stream().map(Estimates::getPrimaryLeafIO).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual PIO (leaves)   : ").append(real.stream().map(Estimates::getPrimaryLeafIO).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected PIO (inner)  : ").append(estimated.stream().map(Estimates::getPrimaryNavigationIO).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual PIO (inner)    : ").append(real.stream().map(Estimates::getPrimaryNavigationIO).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected SIO          : ").append(estimated.stream().map(Estimates::getSecondaryIO).map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual SIO            : ").append(realSIO.stream().map(IOMeasuring::formatMiB).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected Intervals    : ").append(estimated.stream().map(Estimates::getReplayIntervals).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual Intervals      : ").append(real.stream().map(Estimates::getReplayIntervals).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected Raw Intervals: ").append(estimated.stream().map(Estimates::getUnmergedIntervals).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual Raw Intervals  : ").append(real.stream().map(Estimates::getUnmergedIntervals).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Expected Coverage     : ").append(estimated.stream().map(Estimates::getTemporalCoverage).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual Coverage       : ").append(real.stream().map(Estimates::getTemporalCoverage).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Errors PIO            : ").append(pioErrors).append(String.format("%n"));
            result.append("  Errors PIO (leaves)   : ").append(pioLeafErrors).append(String.format("%n"));
            result.append("  Errors PIO (inner)    : ").append(pioInnerErrors).append(String.format("%n"));
            result.append("  Errors SIO            : ").append(sioErrors).append(String.format("%n"));
            result.append("  Error PIO             : ").append(errAccumPIO).append(String.format("%n"));
            result.append("  Error PIO (leaves)    : ").append(errAccumPIOLeaf).append(String.format("%n"));
            result.append("  Error PIO (inner)     : ").append(errAccumPIOInner).append(String.format("%n"));
            result.append("  Error SIO             : ").append(errAccumSIO).append(String.format("%n"));
            result.append("  Expected Score        : ").append(estimated.stream().map(Estimates::getScore).collect(Collectors.toList())).append(String.format("%n"));
            result.append("  Actual Score          : ").append(real.stream().map(Estimates::getScore).collect(Collectors.toList())).append(String.format("%n"));
            result.append("]");
            return result.toString();
        }

        private List<Double> calcSIOErrors() {

            if (estimated.size() != realSIO.size())
                throw new RuntimeException("Unequal list lengths");

            List<Double> result = new ArrayList<>(estimated.size());
            var li = estimated.iterator();
            var ri = realSIO.iterator();
            while (li.hasNext()) {
                Long l = li.next().getSecondaryIO();
                Long r = ri.next();

                long delta = Math.abs(r - l);
                if (delta == 0)
                    result.add(0.0);
                else if (r > 0)
                    result.add((double) delta / r);

            }
            return result;

        }

        private List<Double> calcErrors(Function<Estimates, Long> extract) {

            if (estimated.size() != real.size())
                throw new RuntimeException("Unequal list lengths");

            List<Double> result = new ArrayList<>(estimated.size());
            var li = estimated.iterator();
            var ri = real.iterator();
            while (li.hasNext()) {
                Long l = extract.apply(li.next());
                Long r = extract.apply(ri.next());

                long delta = Math.abs(r - l);
                if (delta == 0)
                    result.add(0.0);
                else if (r > 0)
                    result.add((double) delta / r);

            }
            return result;
        }
    }
}
