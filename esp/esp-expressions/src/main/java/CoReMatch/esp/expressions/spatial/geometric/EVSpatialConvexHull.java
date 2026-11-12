package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 * Calculates the convex hull of the input geometry.
 *
 *
 */
public class EVSpatialConvexHull extends EVUnarySpatialExpression {

    public EVSpatialConvexHull(EVSpatialExpression input) {
        super(input, (Geometry x) -> x.convexHull());
    }

}
