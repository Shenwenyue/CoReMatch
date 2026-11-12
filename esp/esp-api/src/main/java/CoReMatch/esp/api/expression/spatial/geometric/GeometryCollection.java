package CoReMatch.esp.api.expression.spatial.geometric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;

public class GeometryCollection extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param x the inputs
     */
    public GeometryCollection(ArithmeticExpression... x) {
        super(x);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException {
        return DataType.GEOMETRY;
    }
}

