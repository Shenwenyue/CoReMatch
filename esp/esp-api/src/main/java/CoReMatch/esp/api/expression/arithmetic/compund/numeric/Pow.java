package CoReMatch.esp.api.expression.arithmetic.compund.numeric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;

/**
 * Computes a^b
 */
public class Pow extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param a the first input
     * @param b the second input
     */
    public Pow(ArithmeticExpression a, ArithmeticExpression b) {
        super(a, b);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException {
        return DataType.DOUBLE;
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + "^" + getInput(1) + ")";
    }
}
