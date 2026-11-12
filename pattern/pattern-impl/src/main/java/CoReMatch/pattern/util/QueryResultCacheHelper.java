package CoReMatch.pattern.util;

import CoReMatch.db.core.primaryindex.queries.range.CompositeAttributeRange;
import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.EventID;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class QueryResultCacheHelper {

    private QueryResultCacheHelper() {
    }

    public static CachedCursor getOrLoad(
            Map<String, CachedCursor> cache,
            String cacheKey,
            Supplier<Cursor<Pair<?, EventID>>> querySupplier) {
        CachedCursor cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        PhaseTimers.enterPhase2();
        try {
            Cursor<Pair<?, EventID>> queryCursor;
            try {
                queryCursor = querySupplier.get();
            } catch (Exception e) {
                queryCursor = new xxl.core.cursors.sources.EmptyCursor<>();
            }

            CachedCursor materialized = new CachedCursor(queryCursor != null ? queryCursor : new xxl.core.cursors.sources.EmptyCursor<>());

            if (QueryResultCacheKeys.keyRequiresSorting(cacheKey) && materialized.size() > 0) {
                List<Pair<?, EventID>> results = new ArrayList<>(materialized.getCachedResults());
                results = KWayMergeSort.mergeSortDescending(results);
                materialized = new CachedCursor(new xxl.core.cursors.sources.ArrayCursor<>(results.toArray(new Pair[0])));
            }

            cache.put(cacheKey, materialized);
            return materialized;
        } finally {
            PhaseTimers.exitPhase2();
        }
    }

    public static long countAll(CachedCursor cached) {
        if (cached == null) {
            return 0L;
        }
        return cached.size();
    }

    public static long countUnionDistinct(List<CachedCursor> branches) {
        if (branches == null || branches.isEmpty()) {
            return 0L;
        }
        Set<EventID> unionEventIDs = new HashSet<>();
        for (CachedCursor branch : branches) {
            if (branch == null) {
                continue;
            }
            for (Object obj : branch.getCachedResults()) {
                Pair<?, EventID> pair = (Pair<?, EventID>) obj;
                unionEventIDs.add(pair.getElement2());
            }
        }
        return unionEventIDs.size();
    }

    public static CachedCursor viewForUse(CachedCursor original) {
        if (original == null) {
            return new CachedCursor(new xxl.core.cursors.sources.EmptyCursor<>());
        }
        return CachedCursor.sharedView(original);
    }

    public static CachedCursor copyForUse(CachedCursor original) {
        List<Pair<?, EventID>> cachedResults = new ArrayList<>();
        for (Object obj : original.getCachedResults()) {
            cachedResults.add((Pair<?, EventID>) obj);
        }
        return new CachedCursor(new xxl.core.cursors.sources.ArrayCursor<>(cachedResults.toArray(new Pair[0])));
    }

    public static CachedCursor loadCompositeBranch(
            Map<String, CachedCursor> cache,
            CompositeSecondaryTimeIndex<?, ?> compositeIdx,
            CompositeAttributeRange<?, ?> compositeRange) {
        boolean needsSorting = QueryResultCacheKeys.needsSorting(compositeRange);
        String queryKey = QueryResultCacheKeys.compositeKey(compositeIdx, compositeRange, needsSorting);
        return getOrLoad(cache, queryKey, () -> {
            try {
                java.lang.reflect.Method method = compositeIdx.getClass().getMethod(
                    "rangeQueryID", Comparable.class, Comparable.class);
                @SuppressWarnings({"unchecked", "rawtypes"})
                Cursor<Pair<?, EventID>> newQueryResult = (Cursor<Pair<?, EventID>>) method.invoke(
                    compositeIdx, compositeRange.getLower(), compositeRange.getUpper());
                return newQueryResult;
            } catch (Exception e) {
                return new xxl.core.cursors.sources.EmptyCursor<>();
            }
        });
    }

    public static CachedCursor buildConjunctionCursor(
            List<CachedCursor> branches,
            Map<String, CachedCursor> cache,
            String conjKey) {
        CachedCursor existing = cache.get(conjKey);
        if (existing != null) {
            return existing;
        }
        PhaseTimers.enterPhase2();
        try {
            List<List<Pair<?, EventID>>> branchLists = new ArrayList<>();
            for (CachedCursor branch : branches) {
                if (branch != null && branch.size() > 0) {
                    branchLists.add(branch.getCachedResults());
                }
            }
            List<Pair<?, EventID>> conjList = KWayMergeSort.intersectDescendingByEventId(branchLists);
            CachedCursor conjCursor = new CachedCursor(conjList);
            cache.put(conjKey, conjCursor);
            return conjCursor;
        } finally {
            PhaseTimers.exitPhase2();
        }
    }

    public static CachedCursor buildOrUnionCursor(
            List<CachedCursor> branches,
            Map<String, CachedCursor> cache,
            String unionKey) {
        CachedCursor existing = cache.get(unionKey);
        if (existing != null) {
            return existing;
        }
        PhaseTimers.enterPhase2();
        try {
            List<List<Pair<?, EventID>>> branchLists = new ArrayList<>();
            for (CachedCursor branch : branches) {
                if (branch != null && branch.size() > 0) {
                    branchLists.add(branch.getCachedResults());
                }
            }
            List<Pair<?, EventID>> unionList = KWayMergeSort.mergeUnionDescending(branchLists);
            CachedCursor unionCursor = new CachedCursor(unionList);
            cache.put(unionKey, unionCursor);
            return unionCursor;
        } finally {
            PhaseTimers.exitPhase2();
        }
    }

    public static void putBranchList(Map<String, CachedCursor> cache, String cacheKey, List<Pair<?, EventID>> branch) {
        if (branch == null) {
            branch = new ArrayList<>();
        }
        cache.put(cacheKey, new CachedCursor(new xxl.core.cursors.sources.ArrayCursor<>(branch.toArray(new Pair[0]))));
    }

    public static List<Pair<?, EventID>> copyPairsFromCache(CachedCursor cached) {
        if (cached == null) {
            return null;
        }
        List<Pair<?, EventID>> copy = new ArrayList<>();
        for (Object obj : cached.getCachedResults()) {
            copy.add((Pair<?, EventID>) obj);
        }
        return copy;
    }
}
