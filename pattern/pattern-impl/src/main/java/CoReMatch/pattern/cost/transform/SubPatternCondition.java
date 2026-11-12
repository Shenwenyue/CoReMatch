package CoReMatch.pattern.cost.transform;

import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;

import java.util.Objects;

public class SubPatternCondition<T extends Comparable<T>> {

    private final char symbol;
    private final ConditionId id;
    private final int subPatternIndex;
    private final int absolutePosition;
    private final int conditionIndex;
    private final AttributeRange<T> range;
    private boolean enabled;

    public SubPatternCondition(char symbol, int spIndex, int absolutePosition, int conditionIndex, AttributeRange<T> range) {
        this.symbol = symbol;
        this.id = new ConditionId(spIndex, absolutePosition, conditionIndex);
        this.subPatternIndex = spIndex;
        this.absolutePosition = absolutePosition;
        this.conditionIndex = conditionIndex;
        this.range = range;
        this.enabled = true;
    }

    public char getSymbol() {
        return symbol;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public ConditionId getId() {
        return id;
    }

    public AttributeRange<T> getRange() {
        return this.range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(absolutePosition, range, subPatternIndex, conditionIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubPatternCondition other = (SubPatternCondition) obj;
        return this.absolutePosition == other.absolutePosition && Objects.equals(range, other.range) &&
                this.subPatternIndex == other.subPatternIndex && this.conditionIndex == other.conditionIndex;
    }

    @Override
    public String toString() {
        return String.format("(%d, %s ,%s)", this.absolutePosition, this.enabled, this.range);
    }

    public static class ConditionId {

        public final int subPatternIndex;
        public final int absolutePosition;
        public final int conditionIndex;

        public ConditionId(int subPatternIndex, int absolutePosition, int conditionIndex) {
            this.subPatternIndex = subPatternIndex;
            this.absolutePosition = absolutePosition;
            this.conditionIndex = conditionIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(absolutePosition, conditionIndex, subPatternIndex);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConditionId other = (ConditionId) obj;
            return this.absolutePosition == other.absolutePosition && this.conditionIndex == other.conditionIndex &&
                    this.subPatternIndex == other.subPatternIndex;
        }

        public boolean isSameSymbol(ConditionId other) {
            return subPatternIndex == other.subPatternIndex && absolutePosition == other.absolutePosition;
        }

        public boolean isSameSubPattern(ConditionId other) {
            return subPatternIndex == other.subPatternIndex;
        }

        @Override
        public String toString() {
            return String.format("ConditionId [subPatternIndex: %s, absolutePosition: %s, conditionIndex: %s]",
                    this.subPatternIndex, this.absolutePosition, this.conditionIndex);
        }

    }
}
