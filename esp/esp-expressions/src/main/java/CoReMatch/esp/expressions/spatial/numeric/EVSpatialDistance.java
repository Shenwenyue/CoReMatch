package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

/**
 *  Calculates the distance between the two input geometries.
 *
 *
 */
public class EVSpatialDistance extends EVBinarySpatialNumericExpression {

    public EVSpatialDistance(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, DataType.DOUBLE, (Geometry x, Geometry y) -> x.distance(y));
    }
}
