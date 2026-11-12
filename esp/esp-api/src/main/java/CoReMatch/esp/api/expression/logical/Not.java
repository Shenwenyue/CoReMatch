package CoReMatch.esp.api.expression.logical;

import CoReMatch.esp.api.expression.BooleanExpression;
import CoReMatch.esp.api.expression.LogicalExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Logical NOT. Consumes a boolean expression and negates its result
 */
public class Not extends AbstractExpression<BooleanExpression> implements LogicalExpression {

    /**
     * @param input the input expression
     */
    public Not(BooleanExpression input) {
        super(input);
    }
}
