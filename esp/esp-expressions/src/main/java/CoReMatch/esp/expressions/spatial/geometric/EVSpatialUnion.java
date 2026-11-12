package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 *  Calculates the union of the given input geometries.
 *
 *
 */
public class EVSpatialUnion extends EVBinarySpatialExpression {

    public EVSpatialUnion(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, (Geometry x, Geometry y) -> x.union(y));
    }

}
