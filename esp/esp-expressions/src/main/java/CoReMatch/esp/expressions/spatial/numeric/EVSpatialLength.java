package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Calculates the length of the input geometry.
 *
 *
 */
public class EVSpatialLength extends EVUnarySpatialNumericExpression {

    public EVSpatialLength(EVSpatialExpression input) {
        super(input, DataType.DOUBLE, (Geometry x) -> x.getLength());
    }
}
