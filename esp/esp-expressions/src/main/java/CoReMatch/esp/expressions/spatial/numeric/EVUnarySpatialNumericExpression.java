package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVUnarySpatialNumericExpression extends EVAbstractUnaryExpression<Geometry, Number>
        implements EVNumericExpression {

    private final DataType type;

    public EVUnarySpatialNumericExpression(EvaluableExpression<Geometry> input, DataType type, OperatorImpl<Geometry, Number> impl) {
        super(input, impl);
        this.type = type;
    }


    @Override
    public DataType getType() {
        return type;
    }
}

