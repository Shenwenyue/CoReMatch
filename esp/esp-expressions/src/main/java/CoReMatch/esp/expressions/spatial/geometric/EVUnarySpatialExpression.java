package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVUnarySpatialExpression extends EVAbstractUnaryExpression<Geometry, Geometry>
        implements EVSpatialExpression {

    public EVUnarySpatialExpression(EvaluableExpression<Geometry> input, OperatorImpl<Geometry, Geometry> impl) {
        super(input, impl);
    }

    @Override
    public DataType getType() {
        return DataType.GEOMETRY;
    }
}
