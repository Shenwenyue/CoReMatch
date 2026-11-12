package CoReMatch.esp.api.expression.spatial.geometric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;

/**
 * Retrieves the n-th geometry out of a geometry collection. The input geometry
 * is returned if it is not a collection.
 *
 *
 */
public class GeometryN extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    private final int n;

    /**
     * @param x the input
     */
    public GeometryN(ArithmeticExpression x, int n) {
        super(x);
        this.n = n;
    }

    public int getN() {
        return n;
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException {
        return DataType.GEOMETRY;
    }
}

