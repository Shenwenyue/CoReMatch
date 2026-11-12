package CoReMatch.pattern.cost.selection.impl;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.pattern.cost.execution.ExecutionStrategy;
import CoReMatch.pattern.cost.selection.IndexSelectionStrategy;
import CoReMatch.pattern.cost.selection.PatternStats;
import CoReMatch.pattern.cost.selection.CostEstimator;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.util.CachedCursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NaiveStrategySelector implements IndexSelectionStrategy {

    private final TransformedPattern pattern;

    private final PatternStats lwResult;

    private final CostEstimator estimator;

    public NaiveStrategySelector(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                                 TransformedPattern pattern, PatternStats result) {
        this(primary, secondaries, pattern, result, null);
    }

    public NaiveStrategySelector(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                                 TransformedPattern pattern, PatternStats result,
                                 Map<String, CachedCursor> sharedQueryResultCache) {
        this.pattern = pattern;
        this.lwResult = result;
        this.estimator = new CostEstimator(primary, secondaries, pattern, result, sharedQueryResultCache);

    }

    public List<ExecutionStrategy> getAllStrategies() {
        List<ExecutionStrategy> result = new ArrayList<>();
        result.add(estimator.createLightweightStrategy());
        if (!lwResult.getMatchIntervals().isEmpty()) {
            var configs = pattern.iterateConfigurations();

            while (configs.hasNext()) {
                result.add(estimator.createSecondaryStrategy(configs.next()));

            }
        }
        return result;
    }

    @Override
    public Result selectIndexes() {
        long configCounter = 0L;

        ExecutionStrategy cmp = estimator.createLightweightStrategy();

        long time = -System.nanoTime();
        if (!lwResult.getMatchIntervals().isEmpty()) {
            var configs = pattern.iterateConfigurations();
            while (configs.hasNext()) {
                configCounter++;
                ExecutionStrategy tmp = estimator.createSecondaryStrategy(configs.next());
                if (tmp.compareTo(cmp) < 0)
                    cmp = tmp;
            }
        }
        time += System.nanoTime();
        return new Result(cmp, configCounter, time / 1_000_000);
    }

    @Override
    public String getName() {
        return "Naive";
    }
}
