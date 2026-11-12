package CoReMatch.pattern.cost.estimation;

import CoReMatch.pattern.cost.transform.TransformedPattern.ExecutableCondition;

public class ConditionInfo {

    private final ExecutableCondition condition;
    private final double selectivity;

    public ConditionInfo(ExecutableCondition condition, double selectivity) {
        this.condition = condition;
        this.selectivity = selectivity;
    }

    public boolean isHit() {
        return selectivity > 0;
    }

    /**
     * @return the condition
     */
    public ExecutableCondition getCondition() {
        return this.condition;
    }

    /**
     * @return the selectivity
     */
    public double getSelectivity() {
        return this.selectivity;
    }

}
