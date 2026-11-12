package CoReMatch.esp.api.expression.spatial.factory;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class SpatialLineProjection extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    public SpatialLineProjection(ArithmeticExpression... input) {
        super(input);
    }

    @Override
    public Attribute.DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        return Attribute.DataType.GEOMETRY;
    }

    @Override
    public String toString() {
        if (getArity() == 0)
            return "ST_LineProjection()";
        StringBuilder result = new StringBuilder();
        result.append("ST_LineProjection(");
        result.append(getInput(0).toString());
        for (int i = 1; i < getArity(); i++) {
            result.append(", ");
            result.append(getInput(i).toString());
        }
        result.append(")");
        return result.toString();
    }
}
