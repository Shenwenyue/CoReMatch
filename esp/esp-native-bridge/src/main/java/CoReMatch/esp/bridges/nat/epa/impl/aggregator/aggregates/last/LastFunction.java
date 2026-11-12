package CoReMatch.esp.bridges.nat.epa.impl.aggregator.aggregates.last;

import CoReMatch.esp.bridges.nat.epa.impl.aggregator.aggregates.AggregateFunction;

public class LastFunction<T> implements AggregateFunction<T, T, T> {

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
        return right;
    }

    @Override
    public T fEval(T value, long counter) {
        return value;
    }

}
