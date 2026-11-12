package CoReMatch.esp.api.expression.arithmetic.compund.numeric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

/**
 * Computes |x|
 */
public class Abs extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param x the input
     */
    public Abs(ArithmeticExpression x) {
        super(x);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        DataType it = getInput(0).getDataType(schema, bindings);
        if (it.isNumeric())
            return it;
        else
            throw new IncompatibleTypeException("Abs can only be applied to numeric types, input is of type: " + it);
    }
}
