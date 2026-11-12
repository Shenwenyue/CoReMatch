package CoReMatch.pattern.util;

import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.CompositeAttributeRange;
import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Canonical cache keys for shared query result caches (stats + execution).
 */
public final class QueryResultCacheKeys {

    private QueryResultCacheKeys() {
    }

    public static boolean needsSorting(AttributeRange<?> range) {
        return !range.getLower().equals(range.getUpper());
    }

    public static boolean needsSorting(CompositeAttributeRange<?, ?> range) {
        boolean isPointQuery1 = range.getRange1().getLower().equals(range.getRange1().getUpper());
        boolean isPointQuery2 = range.getRange2().getLower().equals(range.getRange2().getUpper());
        return !(isPointQuery1 && isPointQuery2);
    }

    public static String withSortedSuffix(String baseKey, boolean needsSorting) {
        return needsSorting ? baseKey + "_sorted" : baseKey;
    }

    public static String compositeKey(SecondaryTimeIndex<?> idx, CompositeAttributeRange<?, ?> range, boolean needsSorting) {
        return compositeKey(idx.getClass().getSimpleName(), range, needsSorting);
    }

    /**
     * Build a composite cache key without an index instance (for JSON pre-scan / cross-query plans).
     * {@code indexSimpleName} must match runtime {@code idx.getClass().getSimpleName()}.
     */
    public static String compositeKey(String indexSimpleName, CompositeAttributeRange<?, ?> range, boolean needsSorting) {
        String baseKey = indexSimpleName + "_" + range.toString();
        return withSortedSuffix(baseKey, needsSorting);
    }

    /**
     * @deprecated Use {@link #compositeKey} so stats and execution share the same cache entry.
     */
    @Deprecated
    public static String orBranchKey(SecondaryTimeIndex<?> idx, CompositeAttributeRange<?, ?> range, boolean needsSorting) {
        return compositeKey(idx, range, needsSorting);
    }

    /** Materialized OR union for a sub-pattern + secondary range (stats write, execution read). */
    public static String orUnionCompositeKey(int subPatternIndex, SecondaryTimeIndex<?> idx,
                                             AttributeRange<?> secondaryRange) {
        String baseKey = idx.getClass().getSimpleName() + "_OR_UNION_SP" + subPatternIndex + "_"
            + secondaryRange.toString();
        return baseKey + "_sorted";
    }

    /**
     * Materialized AND conjunction across multiple (PRIMARY, secondary) composite dimensions.
     */
    public static String compositeConjunctionKey(
            int subPatternIndex,
            AttributeRange<?> primaryRange,
            List<CompositeSecondaryTimeIndex<?, ?>> dimensions,
            List<AttributeRange<?>> secondaryRanges) {
        String indexPrefix = dimensions.isEmpty()
            ? "Composite"
            : dimensions.get(0).getClass().getSimpleName();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < dimensions.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing(i -> dimensions.get(i).getAttribute2Name().toUpperCase()));

        StringBuilder sb = new StringBuilder();
        sb.append(indexPrefix).append("_CONJUNCTION_SP").append(subPatternIndex).append("_")
            .append(primaryRange.toString());
        for (int idx : order) {
            CompositeSecondaryTimeIndex<?, ?> dim = dimensions.get(idx);
            AttributeRange<?> secondary = secondaryRanges.get(idx);
            sb.append('_').append(dim.getAttribute2Name().toUpperCase()).append(':')
                .append(secondary.toString());
        }
        return sb.append("_sorted").toString();
    }

    public static String independentKey(SecondaryTimeIndex<?> idx, AttributeRange<?> range, boolean needsSorting) {
        String baseKey = idx.getClass().getSimpleName() + "_" + range.toString();
        return withSortedSuffix(baseKey, needsSorting);
    }

    public static boolean keyRequiresSorting(String cacheKey) {
        return cacheKey.endsWith("_sorted");
    }
}
