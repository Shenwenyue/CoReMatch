package CoReMatch.esp.bridges.nat.epa.impl.aggregator.aggregates.first;

import CoReMatch.esp.bridges.nat.epa.impl.aggregator.aggregates.AggregateFunction;

public class FirstFunction<T> implements AggregateFunction<T, T, T> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public T fInit() {
        return null;
    }

    @Override
    public T fInit(T initialValue) {
        return initialValue;
    }

    @Override
    public T fMerge(T left, T right) {
        if (left == null)
            return right;
        return left;
    }

    @Override
    public T fEval(T value, long counter) {
        return value;
    }

}
