package CoReMatch.esp.api.expression.spatial.temporal;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class SpatialTemporalLineCreator extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    public SpatialTemporalLineCreator(ArithmeticExpression a) {
        super(a);
    }

    @Override
    public Attribute.DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        return Attribute.DataType.GEOMETRY;
    }

    @Override
    public String toString() {
        return "ST_MakeTemporalLine(" + getInput(0) + ")";
    }
}
