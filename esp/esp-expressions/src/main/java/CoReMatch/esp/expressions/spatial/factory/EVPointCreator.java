package CoReMatch.esp.expressions.spatial.factory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;
import CoReMatch.event.Attribute;

public class EVPointCreator extends EVAbstractBinaryExpression<Number, Number, Geometry> implements EVSpatialExpression {

    public EVPointCreator(EvaluableExpression<Number> left, EvaluableExpression<Number> right) {
        super(left, right, new EVAbstractBinaryExpression.OperatorImpl<Number, Number, Geometry>() {
            @Override
            public Geometry apply(Number l, Number r) {
                GeometryFactory geometryFactory = new GeometryFactory();
                return geometryFactory.createPoint(new Coordinate(l.doubleValue(), r.doubleValue()));
            }
        });
    }

    @Override
    public Attribute.DataType getType() {
        return Attribute.DataType.GEOMETRY;
    }

}
