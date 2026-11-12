package CoReMatch.pattern.cost.cursor;

import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.EventID;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.CompositeAttributeRange;
import xxl.core.util.Pair;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.util.KWayMergeSort;
import CoReMatch.pattern.util.QueryResultCacheHelper;
import CoReMatch.pattern.util.QueryResultCacheKeys;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.cost.transform.SubPattern;
import CoReMatch.pattern.cost.transform.SubPatternCondition;
import CoReMatch.pattern.util.TIDCursor;
import CoReMatch.pattern.util.ReverseOrderTIDCursor;
import CoReMatch.pattern.util.Util;
import CoReMatch.db.event.TID;
import CoReMatch.db.util.TimeInterval;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SubPatternCandidateCursor extends AbstractCursor<SubPatternCandidate> {

    private final TransformedPattern.ExecutableSubPattern subPattern;

    private final List<? extends SecondaryTimeIndex<?>> indexes;
    private final EventID[] work;
    private Cursor<EventID>[] cursors;
    private final long head;
    private final long tail;
    private List<TimeInterval> filter;
    private SubPatternCandidate nextResult;

    private final boolean useReverseOrder;

    private Map<String, CachedCursor> queryResultCache;

    private Set<Integer> skipConditionIndices;

    private Set<String> conditionAttributes;

    private int materializedCandidateCount = -1;

    public int getMaterializedCandidateCount() {
        return materializedCandidateCount;
    }

    private void logCandidateList(int count) {
        materializedCandidateCount = count;
    }

    @SuppressWarnings("unchecked")
    public SubPatternCandidateCursor(TransformedPattern.ExecutableSubPattern subPattern, List<? extends SecondaryTimeIndex<?>> indexes, List<TimeInterval> filter) {
        this(subPattern, indexes, filter, false);
    }

    @SuppressWarnings("unchecked")
    public SubPatternCandidateCursor(TransformedPattern.ExecutableSubPattern subPattern, List<? extends SecondaryTimeIndex<?>> indexes, List<TimeInterval> filter, boolean useReverseOrder) {
        this(subPattern, indexes, filter, useReverseOrder, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public SubPatternCandidateCursor(TransformedPattern.ExecutableSubPattern subPattern, List<? extends SecondaryTimeIndex<?>> indexes, List<TimeInterval> filter, boolean useReverseOrder, Map<String, CachedCursor> queryResultCache) {
        this.subPattern = subPattern;
        this.indexes = indexes;
        this.filter = filter;
        this.useReverseOrder = true;

        this.queryResultCache = queryResultCache;

        this.conditionAttributes = new HashSet<>();
        for (var c : subPattern.getConditions()) {
            if (c.isComposite()) {

                this.conditionAttributes.add(c.getCompositeRange().getAttribute1Name().toUpperCase());
                this.conditionAttributes.add(c.getCompositeRange().getAttribute2Name().toUpperCase());
            } else {
                this.conditionAttributes.add(c.getRange().getName().toUpperCase());
            }
        }

        this.skipConditionIndices = new HashSet<>();
        Set<String> usedCompositeIndexes = new HashSet<>();

        for (int iCond = 0; iCond < subPattern.getConditions().size(); iCond++) {
             var c = subPattern.getConditions().get(iCond);

            if (c.isComposite()) {
                continue;
            }

             String condAttrName = c.getRange().getName().toUpperCase();

            for (var idx : indexes) {
                if (idx instanceof CompositeSecondaryTimeIndex) {
                    var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                    String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                    String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
                    String compositeKey = mainKey + "_" + secondKey;

                    if (condAttrName.equals(mainKey) && conditionAttributes.contains(secondKey)) {

                        if (!usedCompositeIndexes.contains(compositeKey)) {

                            for (int j = 0; j < subPattern.getConditions().size(); j++) {
                                if (j == iCond) continue;
                                var jc = subPattern.getConditions().get(j);
                                if (jc.isComposite()) continue;
                                String attrName = jc.getRange().getName().toUpperCase();
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

        List<CompositeDimension> applicableDimensions = collectApplicableCompositeDimensions();
        boolean useCompositeConjunction = !applicableDimensions.isEmpty()
            && (applicableDimensions.size() >= 2 || !hasOrPrimaryConditions());
        if (useCompositeConjunction) {
            initializeWithCompositeConjunction(applicableDimensions);
            this.work = new EventID[mergedCursors.length];
        } else {
            Map<SecondaryTimeIndex<?>, List<Integer>> indexToConditionMap = new HashMap<>();
            List<Integer> skippedNoIndexConditions = new ArrayList<>();

            for (int i = 0; i < subPattern.getConditions().size(); i++) {
                if (skipConditionIndices.contains(i)) {
                    continue;
                }

                var c = subPattern.getConditions().get(i);
                SecondaryTimeIndex<?> idx;

                if (c.isComposite()) {
                    idx = Util.getCompositeIndexForCondition(c, indexes);
                    
                } else {
                    String attrName = c.getRange().getName();
                    idx = null;

                    for (var index : indexes) {
                        if (index instanceof CompositeSecondaryTimeIndex) {
                            var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) index;
                            if (compositeIdx.getAttribute1Name().equalsIgnoreCase(attrName)) {
                                String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
                                if (conditionAttributes.contains(secondKey)) {
                                    idx = compositeIdx;
                                    
                                    break;
                                }
                            }
                        }
                    }

                    if (idx == null) {
                        for (var index : indexes) {
                            if (!(index instanceof CompositeSecondaryTimeIndex) &&
                                index.getAttributeName().equalsIgnoreCase(attrName)) {
                                idx = index;
                                
                                break;
                            }
                        }
                    }

                    if (idx == null) {
                        
                        skippedNoIndexConditions.add(i);
                        continue;
                    }
                }
                indexToConditionMap.computeIfAbsent(idx, k -> new ArrayList<>()).add(i);
            }

            boolean hasSharedIndex = indexToConditionMap.values().stream().anyMatch(list -> list.size() > 1);
            boolean hasSkippedConditions = !skipConditionIndices.isEmpty();
            boolean allConditionsSkipped = indexToConditionMap.isEmpty() && !subPattern.getConditions().isEmpty();

            if (allConditionsSkipped) {
                
                this.mergedCursors = new Cursor[0];
                this.work = new EventID[0];
            } else if (hasSharedIndex || hasSkippedConditions) {
                initializeWithMergedRanges(indexToConditionMap);
                this.work = new EventID[mergedCursors.length];
            } else {
                this.cursors = new Cursor[subPattern.getConditions().size()];
                this.work = new EventID[subPattern.getConditions().size()];
            }
        }

        this.head = subPattern.getConditions().get(0).getId().absolutePosition;
        this.tail = subPattern.getLength() - 1 - subPattern.getConditions().get(subPattern.getConditions().size() - 1).getId().absolutePosition;
    }

    private Cursor<EventID>[] mergedCursors;
    private EventID[] mergedWork;
    private TransformedPattern.ExecutableSubPattern mergedSubPattern;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initializeWithMergedRanges(Map<SecondaryTimeIndex<?>, List<Integer>> indexToConditionMap) {

        List<Cursor<EventID>> cursorList = new ArrayList<>();
        List<TransformedPattern.ExecutableCondition> mergedConditions = new ArrayList<>();

        for (Map.Entry<SecondaryTimeIndex<?>, List<Integer>> entry : indexToConditionMap.entrySet()) {
            SecondaryTimeIndex<?> idx = entry.getKey();
            List<Integer> conditionIndices = entry.getValue();


            List<TransformedPattern.ExecutableCondition> conditionsForThisIndex = new ArrayList<>();
            for (int condIdx : conditionIndices) {
                conditionsForThisIndex.add(subPattern.getConditions().get(condIdx));
            }

            if (conditionsForThisIndex.size() == 1) {
                var c = conditionsForThisIndex.get(0);
                System.out.println(c);

                CachedCursor cachedCursor;

                if (c.isComposite()) {

                    CompositeAttributeRange<?, ?> compositeRange = c.getCompositeRange();

                    boolean needsSorting = QueryResultCacheKeys.needsSorting(compositeRange);

                    String queryKey = QueryResultCacheKeys.compositeKey(idx, compositeRange, needsSorting);

                    if (queryResultCache.containsKey(queryKey)) {
                        
                        cachedCursor = QueryResultCacheHelper.viewForUse(queryResultCache.get(queryKey));
                    } else {
                        
                        CompositeSecondaryTimeIndex<?, ?> compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                        final CompositeAttributeRange<?, ?> rangeForQuery = compositeRange;
                        CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, queryKey, () -> {
                            try {
                                java.lang.reflect.Method method = compositeIdx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                                @SuppressWarnings({"unchecked", "rawtypes"})
                                Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                    compositeIdx,
                                    rangeForQuery.getLower(),
                                    rangeForQuery.getUpper());
                                return newQueryResult;
                            } catch (Exception e) {
                                
                                return new xxl.core.cursors.sources.EmptyCursor<>();
                            }
                        });
                        cachedCursor = QueryResultCacheHelper.viewForUse(shared);
                    }

                } else {

                    if (idx instanceof CompositeSecondaryTimeIndex) {

                        CompositeSecondaryTimeIndex<?, ?> compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                        String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                        String secondKey = compositeIdx.getAttribute2Name().toUpperCase();

                        AttributeRange<?> primaryRange = c.getRange();

                        AttributeRange<?> secondaryRange = null;
                        for (int j = 0; j < subPattern.getConditions().size(); j++) {
                            if (skipConditionIndices.contains(j)) {
                                var jc = subPattern.getConditions().get(j);
                                if (!jc.isComposite() && jc.getRange().getName().toUpperCase().equals(secondKey)) {
                                    secondaryRange = jc.getRange();
                                    
                                    break;
                                }
                            }
                        }

                        if (secondaryRange != null) {

                            CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                primaryRange, secondaryRange);

                            boolean needsSorting = QueryResultCacheKeys.needsSorting(compositeRange);

                            String queryKey = QueryResultCacheKeys.compositeKey(idx, compositeRange, needsSorting);

                            if (queryResultCache.containsKey(queryKey)) {
                                
                                cachedCursor = QueryResultCacheHelper.viewForUse(queryResultCache.get(queryKey));
                            } else {
                                
                                final CompositeAttributeRange<?, ?> rangeForQuery = compositeRange;
                                CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, queryKey, () -> {
                                    try {
                                        java.lang.reflect.Method method = compositeIdx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                                        @SuppressWarnings({"unchecked", "rawtypes"})
                                        Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                            compositeIdx,
                                            rangeForQuery.getLower(),
                                            rangeForQuery.getUpper());
                                        return newQueryResult;
                                    } catch (Exception e) {
                                        
                                        e.printStackTrace();
                                        return new xxl.core.cursors.sources.EmptyCursor<>();
                                    }
                                });
                                cachedCursor = QueryResultCacheHelper.viewForUse(shared);
                            }
                        } else {

                            cachedCursor = new CachedCursor(new xxl.core.cursors.sources.EmptyCursor<>());
                        }
                    } else {
                        boolean needsSorting = QueryResultCacheKeys.needsSorting(c.getRange());

                        String queryKey = QueryResultCacheKeys.independentKey(idx, c.getRange(), needsSorting);

                        if (queryResultCache.containsKey(queryKey)) {
                            
                            cachedCursor = QueryResultCacheHelper.viewForUse(queryResultCache.get(queryKey));
                        } else {
                            
                            final AttributeRange<?> rangeForQuery = c.getRange();
                            CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, queryKey, () -> {
                                try {
                                    java.lang.reflect.Method method = idx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                                    @SuppressWarnings({"unchecked", "rawtypes"})
                                    Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                        idx, rangeForQuery.getLower(), rangeForQuery.getUpper());
                                    return newQueryResult;
                                } catch (Exception e) {
                                    return new xxl.core.cursors.sources.EmptyCursor<>();
                                }
                            });
                            cachedCursor = QueryResultCacheHelper.viewForUse(shared);
                        }
                    }
                }

                Cursor<EventID> baseCursor = useReverseOrder
                    ? new ReverseOrderTIDCursor(cachedCursor, false)
                    : new TIDCursor(cachedCursor);
                if (filter != null && !filter.isEmpty()) {
                    baseCursor = new IntervalFilterCursor(filter.iterator(), baseCursor);
                }
                logCandidateList(cachedCursor.size());
                cursorList.add(baseCursor);
                mergedConditions.add(c);
            } else {

                List<List<Pair<?, EventID>>> unionBranches = new ArrayList<>();
                CachedCursor unionCachedCursor = null;
                AttributeRange<?> orSecondaryRangeForUnionKey = null;
                SecondaryTimeIndex<?> orUnionIndex = idx;

                if (idx instanceof CompositeSecondaryTimeIndex) {
                    CompositeSecondaryTimeIndex<?, ?> compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                    String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
                    String secondKey = compositeIdx.getAttribute2Name().toUpperCase();

                    AttributeRange<?> secondaryRange = null;
                    for (int j = 0; j < subPattern.getConditions().size(); j++) {
                        if (skipConditionIndices.contains(j)) {
                            var jc = subPattern.getConditions().get(j);
                            if (!jc.isComposite() && jc.getRange().getName().toUpperCase().equals(secondKey)) {
                                secondaryRange = jc.getRange();
                                
                                break;
                            }
                        }
                    }

                    if (secondaryRange != null) {
                        orSecondaryRangeForUnionKey = secondaryRange;
                        orUnionIndex = compositeIdx;
                        String unionKey = QueryResultCacheKeys.orUnionCompositeKey(
                            subPattern.getIndex(), compositeIdx, secondaryRange);
                        unionCachedCursor = queryResultCache.get(unionKey);
                        if (unionCachedCursor != null) {
                            
                        } else {
                            List<CachedCursor> branchCursorsForUnion = new ArrayList<>();
                            for (var c : conditionsForThisIndex) {
                                System.out.println(c);
                                CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                                    c.getRange(), secondaryRange);

                                boolean needsSorting = QueryResultCacheKeys.needsSorting(compositeRange);
                                String branchCacheKey = QueryResultCacheKeys.compositeKey(
                                    idx, compositeRange, needsSorting);

                                CachedCursor shared = queryResultCache.get(branchCacheKey);
                                if (shared != null) {
                                    
                                } else {
                                    
                                    final CompositeAttributeRange<?, ?> rangeForQuery = compositeRange;
                                    shared = QueryResultCacheHelper.getOrLoad(queryResultCache, branchCacheKey, () -> {
                                        try {
                                            java.lang.reflect.Method method = compositeIdx.getClass().getMethod(
                                                "rangeQueryID", Comparable.class, Comparable.class);
                                            @SuppressWarnings({"unchecked", "rawtypes"})
                                            Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                                compositeIdx, rangeForQuery.getLower(), rangeForQuery.getUpper());
                                            return newQueryResult;
                                        } catch (Exception e) {
                                            
                                            e.printStackTrace();
                                            return new xxl.core.cursors.sources.EmptyCursor<>();
                                        }
                                    });
                                    
                                }
                                
                                branchCursorsForUnion.add(shared);
                            }
                            
                            unionCachedCursor = QueryResultCacheHelper.buildOrUnionCursor(
                                branchCursorsForUnion, queryResultCache, unionKey);
                            
                        }
                    } else {

                        for (var c : conditionsForThisIndex) {
                            System.out.println(c);
                            boolean branchNeedsSorting = QueryResultCacheKeys.needsSorting(c.getRange());
                            String branchCacheKey = QueryResultCacheKeys.independentKey(idx, c.getRange(), branchNeedsSorting);

                            List<Pair<?, EventID>> sortedBranch = getBranchFromCache(branchCacheKey);
                            if (sortedBranch != null) {
                                unionBranches.add(sortedBranch);
                            } else {
                                final AttributeRange<?> rangeForQuery = c.getRange();
                                CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, branchCacheKey, () -> {
                                    try {
                                        java.lang.reflect.Method method = idx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                                        @SuppressWarnings({"unchecked", "rawtypes"})
                                        Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                            idx, rangeForQuery.getLower(), rangeForQuery.getUpper());
                                        return newQueryResult;
                                    } catch (Exception e) {
                                        System.err.println("Warning: Range query failed for " + rangeForQuery.getName() + ": " + e.getMessage());
                                        return new xxl.core.cursors.sources.EmptyCursor<>();
                                    }
                                });
                                unionBranches.add(shared.getCachedResults());
                            }
                        }
                    }
                } else if (unionCachedCursor == null) {

                    for (var c : conditionsForThisIndex) {
                        System.out.println(c);
                        boolean branchNeedsSorting = QueryResultCacheKeys.needsSorting(c.getRange());
                        String branchCacheKey = QueryResultCacheKeys.independentKey(idx, c.getRange(), branchNeedsSorting);

                        List<Pair<?, EventID>> sortedBranch = getBranchFromCache(branchCacheKey);
                        if (sortedBranch != null) {
                            unionBranches.add(sortedBranch);
                        } else {
                            final AttributeRange<?> rangeForQuery = c.getRange();
                            CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, branchCacheKey, () -> {
                                try {
                                    java.lang.reflect.Method method = idx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                                    @SuppressWarnings({"unchecked", "rawtypes"})
                                    Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                        idx, rangeForQuery.getLower(), rangeForQuery.getUpper());
                                    return newQueryResult;
                                } catch (Exception e) {
                                    System.err.println("Warning: Range query failed for " + rangeForQuery.getName() + ": " + e.getMessage());
                                    return new xxl.core.cursors.sources.EmptyCursor<>();
                                }
                            });
                            unionBranches.add(shared.getCachedResults());
                        }
                    }
                }

                if (unionCachedCursor == null) {
                    List<Pair<?, EventID>> unionList = mergeOrUnionBranchesDescending(unionBranches);
                    unionCachedCursor = new CachedCursor(unionList);
                    if (orSecondaryRangeForUnionKey != null) {
                        String unionKey = QueryResultCacheKeys.orUnionCompositeKey(
                            subPattern.getIndex(), orUnionIndex, orSecondaryRangeForUnionKey);
                        queryResultCache.put(unionKey, unionCachedCursor);
                    }
                }

                Cursor<EventID> baseCursor = useReverseOrder
                    ? new ReverseOrderTIDCursor(unionCachedCursor, false)
                    : new TIDCursor(QueryResultCacheHelper.viewForUse(unionCachedCursor));
                if (filter != null && !filter.isEmpty()) {
                    baseCursor = new IntervalFilterCursor(filter.iterator(), baseCursor);
                }
                logCandidateList(unionCachedCursor.size());
                cursorList.add(baseCursor);

                mergedConditions.add(conditionsForThisIndex.get(0));
            }
        }

        this.mergedCursors = cursorList.toArray(new Cursor[0]);
        this.mergedWork = new EventID[this.mergedCursors.length];

        SubPattern newSource = new SubPattern(
            subPattern.getIndex(),
            subPattern.getLength(),
            true,
            new ArrayList<SubPatternCondition<?>>()
        );
        this.mergedSubPattern = new TransformedPattern.ExecutableSubPattern(newSource, mergedConditions);

        this.cursors = this.mergedCursors;

    }

    private static final class CompositeDimension {
        final CompositeSecondaryTimeIndex<?, ?> compositeIdx;
        final AttributeRange<?> primaryRange;
        final AttributeRange<?> secondaryRange;
        final int primaryConditionIndex;

        CompositeDimension(CompositeSecondaryTimeIndex<?, ?> compositeIdx,
                           AttributeRange<?> primaryRange,
                           AttributeRange<?> secondaryRange,
                           int primaryConditionIndex) {
            this.compositeIdx = compositeIdx;
            this.primaryRange = primaryRange;
            this.secondaryRange = secondaryRange;
            this.primaryConditionIndex = primaryConditionIndex;
        }
    }

    private boolean hasOrPrimaryConditions() {
        Map<String, Integer> primaryCounts = new HashMap<>();
        for (var c : subPattern.getConditions()) {
            if (c.isComposite()) {
                continue;
            }
            String attr = c.getRange().getName().toUpperCase();
            for (var index : indexes) {
                if (index instanceof CompositeSecondaryTimeIndex) {
                    var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) index;
                    if (attr.equals(compositeIdx.getAttribute1Name().toUpperCase())) {
                        primaryCounts.merge(attr, 1, Integer::sum);
                    }
                }
            }
        }
        for (int count : primaryCounts.values()) {
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    private List<CompositeDimension> collectApplicableCompositeDimensions() {
        List<CompositeDimension> dims = new ArrayList<>();
        for (var index : indexes) {
            if (!(index instanceof CompositeSecondaryTimeIndex)) {
                continue;
            }
            CompositeSecondaryTimeIndex<?, ?> compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) index;
            String mainKey = compositeIdx.getAttribute1Name().toUpperCase();
            String secondKey = compositeIdx.getAttribute2Name().toUpperCase();
            if (!conditionAttributes.contains(mainKey) || !conditionAttributes.contains(secondKey)) {
                continue;
            }
            AttributeRange<?> primaryRange = null;
            AttributeRange<?> secondaryRange = null;
            int primaryCondIdx = -1;
            for (int i = 0; i < subPattern.getConditions().size(); i++) {
                var c = subPattern.getConditions().get(i);
                if (c.isComposite()) {
                    continue;
                }
                String attr = c.getRange().getName().toUpperCase();
                if (attr.equals(mainKey)) {
                    primaryRange = c.getRange();
                    primaryCondIdx = i;
                } else if (attr.equals(secondKey)) {
                    secondaryRange = c.getRange();
                }
            }
            if (primaryRange != null && secondaryRange != null) {
                dims.add(new CompositeDimension(compositeIdx, primaryRange, secondaryRange, primaryCondIdx));
            }
        }
        dims.sort(Comparator.comparing(d -> d.compositeIdx.getAttribute2Name().toUpperCase()));
        return dims;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void initializeWithCompositeConjunction(List<CompositeDimension> dimensions) {
        List<CachedCursor> branches = new ArrayList<>();
        List<CompositeSecondaryTimeIndex<?, ?>> dimIndexes = new ArrayList<>();
        List<AttributeRange<?>> secondaryRanges = new ArrayList<>();
        for (CompositeDimension dim : dimensions) {
            CompositeAttributeRange<?, ?> compositeRange = new CompositeAttributeRange<>(
                dim.primaryRange, dim.secondaryRange);
            
            CachedCursor branch = QueryResultCacheHelper.loadCompositeBranch(
                queryResultCache, dim.compositeIdx, compositeRange);
            
            branches.add(branch);
            dimIndexes.add(dim.compositeIdx);
            secondaryRanges.add(dim.secondaryRange);
        }

        String conjKey = QueryResultCacheKeys.compositeConjunctionKey(
            subPattern.getIndex(), dimensions.get(0).primaryRange, dimIndexes, secondaryRanges);
        CachedCursor conjCached = queryResultCache.get(conjKey);
        if (conjCached != null) {
            logCandidateList(conjCached.size());
        } else {
            conjCached = QueryResultCacheHelper.buildConjunctionCursor(branches, queryResultCache, conjKey);
            logCandidateList(conjCached.size());
        }

        CachedCursor view = QueryResultCacheHelper.viewForUse(conjCached);
        Cursor<EventID> baseCursor = useReverseOrder
            ? new ReverseOrderTIDCursor(view, false)
            : new TIDCursor(view);
        if (filter != null && !filter.isEmpty()) {
            baseCursor = new IntervalFilterCursor(filter.iterator(), baseCursor);
        }

        List<Cursor<EventID>> cursorList = new ArrayList<>();
        cursorList.add(baseCursor);
        this.mergedCursors = cursorList.toArray(new Cursor[0]);
        this.mergedWork = new EventID[this.mergedCursors.length];

        List<TransformedPattern.ExecutableCondition> mergedConditions = new ArrayList<>();
        mergedConditions.add(subPattern.getConditions().get(dimensions.get(0).primaryConditionIndex));
        SubPattern newSource = new SubPattern(
            subPattern.getIndex(),
            subPattern.getLength(),
            true,
            new ArrayList<SubPatternCondition<?>>());
        this.mergedSubPattern = new TransformedPattern.ExecutableSubPattern(newSource, mergedConditions);
        this.cursors = this.mergedCursors;
        
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void open() {
        if (isOpened)
            return;

        super.open();

        if (mergedCursors != null) {

            for (int i = 0; i < mergedCursors.length; i++) {
                mergedCursors[i].open();
            }

            for (int i = 0; i < mergedCursors.length; i++) {
                if (!mergedCursors[i].hasNext()) {
                    return;
                }
            }

            for (int i = 0; i < mergedCursors.length; i++) {
                mergedWork[i] = mergedCursors[i].next();
            }

            nextResult = computeNextResult();
            return;
        }

        for (int i = 0; i < cursors.length; i++) {

            var c = subPattern.getConditions().get(i);
            System.out.println(c);

            SecondaryTimeIndex idx = Util.getIndexForRange(c.getRange(), indexes);

            boolean needsSorting = QueryResultCacheKeys.needsSorting(c.getRange());

            String queryKey = QueryResultCacheKeys.independentKey(idx, c.getRange(), needsSorting);

            if (useReverseOrder) {
                if (queryResultCache.containsKey(queryKey)) {
                    
                    List<EventID> eventIdList = new ArrayList<>();
                    for (Pair<?, EventID> pair : queryResultCache.get(queryKey).getCachedResults()) {
                        eventIdList.add(pair.getElement2());
                    }
                    cursors[i] = new ReverseOrderTIDCursor(eventIdList, false);
                } else {
                    
                    final AttributeRange<?> rangeForQuery = c.getRange();
                    CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, queryKey, () -> {
                        Cursor<?> rawCursor = idx.rangeQueryID(rangeForQuery.getLower(), rangeForQuery.getUpper());
                        if (rawCursor instanceof Cursor) {
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            Cursor<Pair<?, EventID>> typedCursor = (Cursor<Pair<?, EventID>>) rawCursor;
                            return typedCursor;
                        }
                        return new xxl.core.cursors.sources.EmptyCursor<>();
                    });
                    cursors[i] = new ReverseOrderTIDCursor(shared, false);
                }
            } else {
                CachedCursor cachedCursor;
                if (queryResultCache.containsKey(queryKey)) {
                    cachedCursor = QueryResultCacheHelper.viewForUse(queryResultCache.get(queryKey));
                } else {
                    final AttributeRange<?> rangeForQuery = c.getRange();
                    CachedCursor shared = QueryResultCacheHelper.getOrLoad(queryResultCache, queryKey, () -> {
                        try {
                            java.lang.reflect.Method method = idx.getClass().getMethod("rangeQueryID", Comparable.class, Comparable.class);
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                                idx, rangeForQuery.getLower(), rangeForQuery.getUpper());
                            return newQueryResult;
                        } catch (Exception e) {
                            return new xxl.core.cursors.sources.EmptyCursor<>();
                        }
                    });
                    cachedCursor = QueryResultCacheHelper.viewForUse(shared);
                }
                cursors[i] = new TIDCursor(cachedCursor);
            }

            if (filter != null && !filter.isEmpty()) {
                cursors[i] = new IntervalFilterCursor(filter.iterator(), cursors[i]);
            }

            cursors[i].open();
        }

        for (int i = 0; i < cursors.length; i++) {
            if (!cursors[i].hasNext()) {

                return;
            }
        }

        for (int i = 0; i < cursors.length; i++) {
            work[i] = cursors[i].next();
        }

        nextResult = computeNextResult();
    }

    @Override
    public void close() {
        if (isClosed)
            return;

        super.close();

        if (mergedCursors != null) {
            for (var c : mergedCursors)
                c.close();
            return;
        }

        for (var c : cursors)
            c.close();
    }

    private SubPatternCandidate computeNextResult() {
        SubPatternCandidate result = null;

        if (mergedCursors != null) {
            return computeNextResultWithMerged();
        }

        outer:
        while (cursors[0].hasNext() && result == null) {
            work[0] = cursors[0].next();

            for (int i = 1; i < subPattern.getConditions().size(); i++) {
                var condition = subPattern.getConditions().get(i);

                final long seqPrev = work[i - 1].getSequenceId();
                long seqMin = seqPrev + condition.getMinDist();

                nextElement(i, seqMin);

                if (work[i] == null)
                    return null;

                final long seqDelta = work[i].getSequenceId() - seqPrev;

                if (seqDelta > condition.getMaxDist())
                    continue outer;
            }

            result = new SubPatternCandidate(
                    new TID(work[0].getBlockId(), work[0].getOffset()),
                    work[0].getTimestamp(),
                    work[work.length - 1].getTimestamp(),

                    work[0].getSequenceId() - head,
                    work[work.length - 1].getSequenceId() + tail);
        }
        return result;
    }

    private SubPatternCandidate computeNextResultWithMerged() {

        if (mergedCursors == null || mergedCursors.length == 0) {
            return null;
        }

        SubPatternCandidate result = null;

        outer:
        while (mergedCursors[0].hasNext() && result == null) {
            mergedWork[0] = mergedCursors[0].next();

            for (int i = 1; i < mergedSubPattern.getConditions().size(); i++) {
                var condition = mergedSubPattern.getConditions().get(i);

                final long seqPrev = mergedWork[i - 1].getSequenceId();
                long seqMin = seqPrev + condition.getMinDist();

                nextElementMerged(i, seqMin);

                if (mergedWork[i] == null)
                    return null;

                final long seqDelta = mergedWork[i].getSequenceId() - seqPrev;

                if (seqDelta > condition.getMaxDist())
                    continue outer;
            }

            result = new SubPatternCandidate(
                    new TID(mergedWork[0].getBlockId(), mergedWork[0].getOffset()),
                    mergedWork[0].getTimestamp(),
                    mergedWork[mergedWork.length - 1].getTimestamp(),

                    mergedWork[0].getSequenceId() - head,
                    mergedWork[mergedWork.length - 1].getSequenceId() + tail);
        }
        return result;
    }

    private void nextElementMerged(int idx, long seqMin) {
        while (mergedWork[idx].getSequenceId() < seqMin && mergedCursors[idx].hasNext())
            mergedWork[idx] = mergedCursors[idx].next();

        if (mergedWork[idx].getSequenceId() < seqMin)
            mergedWork[idx] = null;
    }

    private void nextElement(int idx, long seqMin) {
        while (work[idx].getSequenceId() < seqMin && cursors[idx].hasNext())
            work[idx] = cursors[idx].next();

        if (work[idx].getSequenceId() < seqMin)
            work[idx] = null;
    }

    @Override
    protected boolean hasNextObject() {
        return nextResult != null;
    }

    @Override
    protected SubPatternCandidate nextObject() {
        var res = nextResult;
        nextResult = computeNextResult();
        return res;
    }

    private static boolean isPointRange(AttributeRange<?> range) {
        return range.getLower().equals(range.getUpper());
    }

    private List<Pair<?, EventID>> getBranchFromCache(String cacheKey) {
        CachedCursor cached = queryResultCache.get(cacheKey);
        if (cached == null) {
            return null;
        }
        return cached.getCachedResults();
    }

    private static List<Pair<?, EventID>> mergeOrUnionBranchesDescending(
            List<List<Pair<?, EventID>>> branches) {
        return mergeOrUnionBranchesDescendingLocal(branches);
    }

    private static List<Pair<?, EventID>> mergeOrUnionBranchesDescendingLocal(
            List<List<Pair<?, EventID>>> branches) {
        if (branches == null || branches.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Pair<?, EventID>>> nonEmpty = new ArrayList<>();
        for (List<Pair<?, EventID>> branch : branches) {
            if (branch != null && !branch.isEmpty()) {
                nonEmpty.add(branch);
            }
        }
        if (nonEmpty.isEmpty()) {
            return new ArrayList<>();
        }
        if (nonEmpty.size() == 1) {
            return new ArrayList<>(nonEmpty.get(0));
        }

        int totalSize = 0;
        for (List<Pair<?, EventID>> branch : nonEmpty) {
            totalSize += branch.size();
        }
        ArrayList<Pair<?, EventID>> result = new ArrayList<>(totalSize);
        final int branchCount = nonEmpty.size();

        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> {
                EventID eA = nonEmpty.get(a[0]).get(a[1]).getElement2();
                EventID eB = nonEmpty.get(b[0]).get(b[1]).getElement2();
                return Long.compare(eB.getSequenceId(), eA.getSequenceId());
            }
        );

        for (int i = 0; i < branchCount; i++) {
            maxHeap.offer(new int[]{i, 0});
        }

        EventID lastAdded = null;
        while (!maxHeap.isEmpty()) {
            int[] top = maxHeap.poll();
            int bIdx = top[0];
            int pos = top[1];

            Pair<?, EventID> current = nonEmpty.get(bIdx).get(pos);
            EventID currentId = current.getElement2();

            if (lastAdded == null || !currentId.equals(lastAdded)) {
                result.add(current);
                lastAdded = currentId;
            }

            if (pos + 1 < nonEmpty.get(bIdx).size()) {
                maxHeap.offer(new int[]{bIdx, pos + 1});
            }
        }

        return result;
    }

    private class IntervalFilterCursor extends AbstractCursor<EventID> {
        private Iterator<TimeInterval> filter;
        private Cursor<EventID> data;

        private EventID nextElement;
        private TimeInterval currentFilter;

        public IntervalFilterCursor(Iterator<TimeInterval> filter, Cursor<EventID> data) {
            this.filter = filter;
            this.data = data;
        }

        @Override
        public void open() {
            if (isOpened)
                return;

            super.open();
            data.open();

            if (filter.hasNext())
                currentFilter = filter.next();

            nextElement = computeNext();
        }

        private EventID computeNext() {
            EventID result = null;
            while (result == null && data.hasNext() && currentFilter != null) {
                result = data.next();
                while (currentFilter != null && result.getTimestamp() > currentFilter.getT2()) {
                    currentFilter = filter.hasNext() ? filter.next() : null;
                }

                if (currentFilter != null && !currentFilter.contains(result.getTimestamp()))
                    result = null;
            }
            return result;
        }

        @Override
        public void close() {
            if (isClosed)
                return;

            super.close();
            data.close();
        }

        @Override
        protected boolean hasNextObject() {
            return nextElement != null;
        }

        @Override
        protected EventID nextObject() {
            var res = nextElement;
            nextElement = computeNext();
            return res;
        }
    }

}
