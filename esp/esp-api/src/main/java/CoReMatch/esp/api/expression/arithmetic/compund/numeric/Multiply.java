package CoReMatch.esp.api.expression.arithmetic.compund.numeric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

/**
 * Computes a * b
 */
public class Multiply extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param a the first input
     * @param b the second input
     */
    public Multiply(ArithmeticExpression a, ArithmeticExpression b) {
        super(a, b);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        DataType l = getInput(0).getDataType(schema, bindings);
        DataType r = getInput(1).getDataType(schema, bindings);
        DataType gct = DataType.getGCT(l, r);
        return (gct.compareTo(DataType.INTEGER) < 0) ? DataType.INTEGER : gct;
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + " * " + getInput(1) + ")";
    }

}
