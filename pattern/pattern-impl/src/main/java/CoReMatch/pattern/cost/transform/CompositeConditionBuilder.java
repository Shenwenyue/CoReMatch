package CoReMatch.pattern.cost.transform;

import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.CompositeAttributeRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompositeConditionBuilder {

    public static class CompositeIndexInfo {
        public final String primaryKey;
        public final String secondaryKey;

        public CompositeIndexInfo(String primaryKey, String secondaryKey) {
            this.primaryKey = primaryKey;
            this.secondaryKey = secondaryKey;
        }
    }

    public static class MergeResult {
        public final List<SubPatternCondition<?>> mergedConditions;
        public final List<Integer> skippedConditionIndices;

        public MergeResult(List<SubPatternCondition<?>> mergedConditions, List<Integer> skippedConditionIndices) {
            this.mergedConditions = mergedConditions;
            this.skippedConditionIndices = skippedConditionIndices;
        }
    }

    public static MergeResult mergeConditionsForCompositeIndexes(
            SubPattern subPattern,
            List<CompositeIndexInfo> compositeIndexes) {

        List<SubPatternCondition<?>> conditions = subPattern.getConditions();
        List<SubPatternCondition<?>> resultConditions = new ArrayList<>();
        List<Integer> skippedIndices = new ArrayList<>();

        Map<String, Integer> attrToConditionIndex = new HashMap<>();
        for (int i = 0; i < conditions.size(); i++) {
            String attrName = conditions.get(i).getRange().getName().toUpperCase();
            attrToConditionIndex.put(attrName, i);
        }

        Set<Integer> processedIndices = new HashSet<>();

        for (CompositeIndexInfo idxInfo : compositeIndexes) {
            String primaryKey = idxInfo.primaryKey.toUpperCase();
            String secondaryKey = idxInfo.secondaryKey.toUpperCase();

            Integer primaryIdx = attrToConditionIndex.get(primaryKey);
            Integer secondaryIdx = attrToConditionIndex.get(secondaryKey);

            if (primaryIdx != null && secondaryIdx != null &&
                !processedIndices.contains(primaryIdx) && !processedIndices.contains(secondaryIdx)) {

                SubPatternCondition<?> primaryCond = conditions.get(primaryIdx);
                SubPatternCondition<?> secondaryCond = conditions.get(secondaryIdx);

                SubPatternCondition<?> compositeCond = createCompositeCondition(
                    primaryCond, secondaryCond);

                resultConditions.add(compositeCond);
                processedIndices.add(primaryIdx);
                processedIndices.add(secondaryIdx);
                skippedIndices.add(secondaryIdx);
            }
        }

        for (int i = 0; i < conditions.size(); i++) {
            if (!processedIndices.contains(i)) {
                resultConditions.add(conditions.get(i));
            }
        }

        return new MergeResult(resultConditions, skippedIndices);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SubPatternCondition<?> createCompositeCondition(
            SubPatternCondition<?> primaryCond,
            SubPatternCondition<?> secondaryCond) {

        AttributeRange<?> primaryRange = primaryCond.getRange();
        AttributeRange<?> secondaryRange = secondaryCond.getRange();

        CompositeAttributeRange compositeRange = new CompositeAttributeRange(
            (AttributeRange) primaryRange,
            (AttributeRange) secondaryRange);

        return primaryCond;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TransformedPattern.ExecutableCondition createCompositeExecutableCondition(
            SubPatternCondition<?> primaryCond,
            SubPatternCondition<?> secondaryCond,
            long minDist,
            long maxDist) {

        AttributeRange<?> primaryRange = primaryCond.getRange();
        AttributeRange<?> secondaryRange = secondaryCond.getRange();

        CompositeAttributeRange compositeRange = new CompositeAttributeRange(
            (AttributeRange) primaryRange,
            (AttributeRange) secondaryRange);

        return new TransformedPattern.ExecutableCondition(
            primaryCond, secondaryCond, minDist, maxDist, compositeRange);
    }

    public static boolean canMergeConditions(
            List<SubPatternCondition<?>> conditions,
            List<CompositeIndexInfo> compositeIndexes) {

        Set<String> conditionAttrs = new HashSet<>();
        for (var cond : conditions) {
            conditionAttrs.add(cond.getRange().getName().toUpperCase());
        }

        for (CompositeIndexInfo idxInfo : compositeIndexes) {
            if (conditionAttrs.contains(idxInfo.primaryKey.toUpperCase()) &&
                conditionAttrs.contains(idxInfo.secondaryKey.toUpperCase())) {
                return true;
            }
        }

        return false;
    }
}
