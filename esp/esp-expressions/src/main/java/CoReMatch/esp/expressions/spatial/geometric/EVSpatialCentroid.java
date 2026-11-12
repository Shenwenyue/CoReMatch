package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import CoReMatch.esp.expressions.arithmetic.EVArithmeticExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Calculates centre of mass of the input geometry.
 *
 *
 */
public class EVSpatialCentroid extends EVAbstractUnaryExpression<Geometry, Point>
        implements EVArithmeticExpression<Point> {


    public EVSpatialCentroid(EVSpatialExpression input) {
        super(input, (Geometry x) -> x.getCentroid());
    }


    @Override
    public DataType getType() {
        return DataType.GEOMETRY;
    }

}
