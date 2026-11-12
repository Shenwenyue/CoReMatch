package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import CoReMatch.esp.expressions.arithmetic.EVArithmeticExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Calculates a point inside the area of input geometry.
 *
 *
 */
public class EVSpatialInteriorPoint extends EVAbstractUnaryExpression<Geometry, Point>
        implements EVArithmeticExpression<Point> {

    public EVSpatialInteriorPoint(EVSpatialExpression input) {
        super(input, (Geometry x) -> x.getInteriorPoint());
    }

    @Override
    public DataType getType() {
        return DataType.GEOMETRY;
    }
}
