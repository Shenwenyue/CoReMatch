package CoReMatch.esp.api.expression.predicate;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Checks if the left input is less than the right one
 */
public class Between extends AbstractExpression<ArithmeticExpression> implements Predicate {

    /**
     * @param value the input expression
     * @param lower the lower bound
     * @param upper the upper bound
     */
    public Between(ArithmeticExpression value, ArithmeticExpression lower, ArithmeticExpression upper) {
        super(value, lower, upper);
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") BETWEEN (" + getInput(1) + ") AND (" + getInput(2) + ")";
    }

}
