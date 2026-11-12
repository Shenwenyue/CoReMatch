package CoReMatch.esp.api.expression.predicate.set;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Checks if the first input is superset of the second input
 *
 */
public class Superset extends AbstractExpression<ArithmeticExpression> implements Predicate {

    /**
     * @param left the first input
     * @param right the second input
     */
    public Superset(ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") is superset of (" + getInput(1) + ")";
    }

}
