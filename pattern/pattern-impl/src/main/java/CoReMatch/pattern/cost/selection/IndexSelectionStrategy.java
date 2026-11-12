package CoReMatch.pattern.cost.selection;

import CoReMatch.pattern.cost.execution.ExecutionStrategy;

public interface IndexSelectionStrategy {

    static final boolean TRACE = true;

    public static void trace(String fmt, Object... args) {
        if (TRACE)
            System.out.printf(fmt, args);
    }

    String getName();

    Result selectIndexes();

    public static class Result {
        private final ExecutionStrategy strategy;
        private final long costModeInvocations;
        private final long selectionTimeMillis;

        public Result(ExecutionStrategy strategy, long costModeInvocations, long selectionTimeMillis) {
            this.strategy = strategy;
            this.costModeInvocations = costModeInvocations;
            this.selectionTimeMillis = selectionTimeMillis;
        }

        /**
         * @return the strategy
         */
        public ExecutionStrategy getStrategy() {
            return this.strategy;
        }

        /**
         * @return the costModeInvocations
         */
        public long getCostModelInvocations() {
            return this.costModeInvocations;
        }

        /**
         * @return the selectionTimeMillis
         */
        public long getSelectionTimeMillis() {
            return this.selectionTimeMillis;
        }
    }

}
