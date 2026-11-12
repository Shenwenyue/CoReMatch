package CoReMatch.esp.api.expression.arithmetic.compund.string;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class StringConcatenate extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param left the left input expression
     * @param right the right input expression
     */
    public StringConcatenate(ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
    }

    @Override
    public String toString() {
        return "" + getInput(0) + " " + getInput(1);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings)
            throws IncompatibleTypeException, SchemaException {
        return DataType.STRING;
    }

}

