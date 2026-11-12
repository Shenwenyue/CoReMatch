package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;

/**
 * Calculates the bounding box of the input geometry.
 *
 *
 */
public class EVSpatialEnvelope extends EVUnarySpatialExpression {

    public EVSpatialEnvelope(EVSpatialExpression input) {
        super(input, (Geometry x) -> x.getEnvelope());
    }

}
