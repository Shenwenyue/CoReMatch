package CoReMatch.esp.api.expression.arithmetic.atomic;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.event.Attribute.DataType;

/**
 * Spatial Constant
 *
 *
 */
public class GeoConstant extends Constant<Geometry> {

    /**
     * @param value The value
     */
    public GeoConstant(Geometry value) {
        super(value, DataType.GEOMETRY);
    }

    @Override
    public String toString() {
        return value == null ? "null" : "'" + value + "'";
    }
}
