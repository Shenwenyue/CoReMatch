package CoReMatch.esp.expressions.arithmetic.constant;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;

public class EVSpatialConstant extends EVConstant<Geometry> implements EVSpatialExpression {

    public EVSpatialConstant(Geometry value) {
        super(value, DataType.GEOMETRY);
    }
}

