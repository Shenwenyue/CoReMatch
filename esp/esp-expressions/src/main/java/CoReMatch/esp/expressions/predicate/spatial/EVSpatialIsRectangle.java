package CoReMatch.esp.expressions.predicate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;

/**
 * Tests whether the input geometry is a rectangle.
 */
public class EVSpatialIsRectangle extends EVAbstractUnaryExpression<Geometry, Boolean> implements EVPredicate {

    public EVSpatialIsRectangle(EVSpatialExpression input) {
        super(input, new OperatorImpl<Geometry, Boolean>() {

            @Override
            public Boolean apply(Geometry input) {
                return input.isRectangle();
            }
        });
    }

}

