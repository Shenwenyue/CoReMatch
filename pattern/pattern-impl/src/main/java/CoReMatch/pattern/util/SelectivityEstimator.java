package CoReMatch.pattern.util;

import CoReMatch.esp.api.expression.BooleanExpression;

/**
 * Estimator interface for boolean expression selectivity.
 */
public interface SelectivityEstimator {

    /**
     * Estimates the selectivity of the given boolean expression.
     *
     * @param exp the boolean expression
     * @return the selectivity of the given boolean expression
     */
    double estimateSelectivity(BooleanExpression exp);
}
