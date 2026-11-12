package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Constructs a new SpatialDimension instance
 */
public class EVSpatialDimension extends EVUnarySpatialNumericExpression {

    public EVSpatialDimension(EVSpatialExpression input) {
        super(input, DataType.INTEGER, (Geometry x) -> x.getDimension());
    }
}
