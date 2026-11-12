package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Calculates boundary-dimension of the input geometry.
 */
public class EVSpatialBoundaryDimension extends EVUnarySpatialNumericExpression {

    public EVSpatialBoundaryDimension(EVSpatialExpression input) {
        super(input, DataType.INTEGER, (Geometry x) -> x.getBoundaryDimension());
    }
}
