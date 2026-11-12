package CoReMatch.pattern.cost.selection.impl;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.pattern.cost.execution.ExecutionStrategy;
import CoReMatch.pattern.cost.selection.CostEstimator;
import CoReMatch.pattern.cost.selection.IndexSelectionStrategy;
import CoReMatch.pattern.cost.selection.PatternStats;
import CoReMatch.pattern.cost.transform.TransformedPattern;

import java.util.List;

public class AllStrategySelector implements IndexSelectionStrategy {

    private final TransformedPattern pattern;

    private final CostEstimator estimator;

    public AllStrategySelector(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                               TransformedPattern pattern, PatternStats result) {
        this.pattern = pattern;
        this.estimator = new CostEstimator(primary, secondaries, pattern, result);
    }

    @Override
    public String getName() {
        return "All";
    }

    @Override
    public Result selectIndexes() {
        long time = -System.currentTimeMillis();
        pattern.enableAll();
        pattern.createExecution();
        ExecutionStrategy strategy = estimator.createSecondaryStrategy(pattern.createExecution());
        time += System.currentTimeMillis();
        return new Result(strategy, 1, time);
    }

}
