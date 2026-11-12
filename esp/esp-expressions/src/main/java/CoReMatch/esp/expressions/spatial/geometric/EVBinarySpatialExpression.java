package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVBinarySpatialExpression extends EVAbstractBinaryExpression<Geometry, Geometry, Geometry>
        implements EVSpatialExpression {

    public EVBinarySpatialExpression(EvaluableExpression<Geometry> left, EvaluableExpression<Geometry> right, OperatorImpl<Geometry, Geometry, Geometry> impl) {
        super(left, right, impl);
    }

    @Override
    public DataType getType() {
        return DataType.GEOMETRY;
    }

}

