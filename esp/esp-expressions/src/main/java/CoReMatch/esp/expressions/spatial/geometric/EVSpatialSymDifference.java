package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 * Calculates the symmetric difference of the given input geometries.
 *
 *
 */
public class EVSpatialSymDifference extends EVBinarySpatialExpression {

    public EVSpatialSymDifference(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, (Geometry x, Geometry y) -> x.symDifference(y));
    }

}
