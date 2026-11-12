package CoReMatch.esp.expressions.predicate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;


/**
 * Tests whether the first input geometry touches the second one.
 */
public class EVSpatialTouches extends EVAbstractBinaryExpression<Geometry, Geometry, Boolean> implements EVPredicate {

    public EVSpatialTouches(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, new OperatorImpl<Geometry, Geometry, Boolean>() {

            @Override
            public Boolean apply(Geometry l, Geometry r) {
                return l.touches(r);
            }
        });
    }

}
