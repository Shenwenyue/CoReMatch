package CoReMatch.esp.api.expression.logical;

import CoReMatch.esp.api.expression.BooleanExpression;
import CoReMatch.esp.api.expression.LogicalExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Logical AND: Evaluates to true iff both input-expression evaluate to true.
 */
public class And extends AbstractExpression<BooleanExpression> implements LogicalExpression {

    /**
     * @param left  the first input
     * @param right the second input
     */
    public And(BooleanExpression left, BooleanExpression right) {
        super(left, right);
    }
}
