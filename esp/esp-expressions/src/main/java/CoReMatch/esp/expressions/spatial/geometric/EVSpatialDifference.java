package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 * Calculates the difference of the given input geometries.
 *
 *
 */
public class EVSpatialDifference extends EVBinarySpatialExpression {

    public EVSpatialDifference(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, (Geometry x, Geometry y) -> x.difference(y));
    }

}
