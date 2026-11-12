package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVBinarySpatialNumericExpression extends EVAbstractBinaryExpression<Geometry, Geometry, Number>
        implements EVNumericExpression {

    private final DataType type;

    public EVBinarySpatialNumericExpression(EvaluableExpression<Geometry> left, EvaluableExpression<Geometry> right, DataType type, OperatorImpl<Geometry, Geometry, Number> impl) {
        super(left, right, impl);
        this.type = type;
    }

    @Override
    public DataType getType() {
        return type;
    }

}
