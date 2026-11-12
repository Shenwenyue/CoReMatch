package CoReMatch.esp.api.expression.predicate.spatial;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Checks if the left input intersects the right one
 */
public class Intersects extends AbstractExpression<ArithmeticExpression> implements Predicate {

    /**
     * @param left  the first input
     * @param right the second input
     */
    public Intersects(ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") intersects (" + getInput(1) + ")";
    }

}
