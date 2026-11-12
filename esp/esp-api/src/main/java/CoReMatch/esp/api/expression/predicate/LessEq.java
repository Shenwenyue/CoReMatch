package CoReMatch.esp.api.expression.predicate;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Checks if the left input is less than or equal to the right one
 */
public class LessEq extends AbstractExpression<ArithmeticExpression> implements Predicate {

    /**
     * @param left  the first input
     * @param right the second input
     */
    public LessEq(ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") <= (" + getInput(1) + ")";
    }
}
