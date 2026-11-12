package CoReMatch.esp.expressions.spatial.numeric;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

/**
 * Calculates area of the input geometry.
 */
public class EVSpatialArea extends EVUnarySpatialNumericExpression {

    public EVSpatialArea(EVSpatialExpression input) {
        super(input, DataType.DOUBLE, (Geometry x) -> x.getArea());
    }

}
