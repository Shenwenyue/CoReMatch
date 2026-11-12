package CoReMatch.esp.api.expression.predicate;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Compares two inputs for Like Matching.
 */
public class Like extends AbstractExpression<ArithmeticExpression> implements Predicate {

    /**
     * @param left  the input
     * @param right the pattern
     */
    public Like(ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") LIKE (" + getInput(1) + ")";
    }

}
