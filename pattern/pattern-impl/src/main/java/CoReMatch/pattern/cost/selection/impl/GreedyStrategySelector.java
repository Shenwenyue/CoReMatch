package CoReMatch.pattern.cost.selection.impl;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.pattern.cost.execution.ExecutionStrategy;
import CoReMatch.pattern.cost.selection.CostEstimator;
import CoReMatch.pattern.cost.selection.IndexSelectionStrategy;
import CoReMatch.pattern.cost.selection.PatternStats;
import CoReMatch.pattern.cost.selection.SelSubPattern;
import CoReMatch.pattern.cost.transform.TransformedPattern;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class GreedyStrategySelector implements IndexSelectionStrategy {

    private final TransformedPattern pattern;

    private final PatternStats lwResult;

    private final CostEstimator estimator;

    public GreedyStrategySelector(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                                  TransformedPattern pattern, PatternStats result) {
        this.pattern = pattern;
        this.lwResult = result;
        this.estimator = new CostEstimator(primary, secondaries, pattern, result);
    }

    @Override
    public Result selectIndexes() {

        List<SelSubPattern.ICondition> conditions = estimator.getSelectivityEnrichedSubPatterns().stream().flatMap(x -> x.getConditions().stream()).collect(Collectors.toList());

        Collections.sort(conditions);

        ExecutionStrategy cmp = estimator.createLightweightStrategy();

        long configCounter = 0L;

        long time = -System.nanoTime();
        if (!lwResult.getMatchIntervals().isEmpty()) {
            pattern.disableAll();
            for (Iterator<SelSubPattern.ICondition> ci = conditions.iterator(); ci.hasNext(); ) {
                configCounter++;
                ci.next().getCondition().enable();
                ExecutionStrategy tmp = estimator.createSecondaryStrategy(pattern.createExecution());
                if (tmp.compareTo(cmp) < 0)
                    cmp = tmp;
            }
        }
        time += System.nanoTime();
        return new Result(cmp, configCounter, time / 1_000_000);
    }

    @Override
    public String getName() {
        return "Greedy";
    }
}
