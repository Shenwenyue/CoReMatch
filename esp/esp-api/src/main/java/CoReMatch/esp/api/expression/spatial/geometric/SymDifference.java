package CoReMatch.esp.api.expression.spatial.geometric;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;

/**
 * Calculates the symmetric difference of the given input geometries.
 *
 *
 */
public class SymDifference extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /**
     * @param x the first input
     * @param y the seond input
     */
    public SymDifference(ArithmeticExpression x, ArithmeticExpression y) {
        super(x, y);
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException {
        return DataType.GEOMETRY;
    }
}
