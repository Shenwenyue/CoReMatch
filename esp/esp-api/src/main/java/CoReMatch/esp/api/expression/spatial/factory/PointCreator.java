package CoReMatch.esp.api.expression.spatial.factory;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class PointCreator extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    public PointCreator(ArithmeticExpression a, ArithmeticExpression b) {
        super(a, b);
    }

    @Override
    public Attribute.DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        return Attribute.DataType.GEOMETRY;
    }

    @Override
    public String toString() {
        return "asPoint(" + getInput(0) + ", " + getInput(1) + ")";
    }
}
