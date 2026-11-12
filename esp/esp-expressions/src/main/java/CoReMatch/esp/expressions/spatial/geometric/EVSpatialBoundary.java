package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 * Calculates boundary of the input geometry.
 *
 *
 */
public class EVSpatialBoundary extends EVUnarySpatialExpression {

    public EVSpatialBoundary(EVSpatialExpression input) {
        super(input, (Geometry x) -> x.getBoundary());
    }

}
