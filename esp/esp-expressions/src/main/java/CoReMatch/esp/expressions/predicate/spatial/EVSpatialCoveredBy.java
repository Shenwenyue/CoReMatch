package CoReMatch.esp.expressions.predicate.spatial;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;

/**
 * Tests whether the first input geometry is covered by the second one.
 */
public class EVSpatialCoveredBy extends EVAbstractBinaryExpression<Geometry, Geometry, Boolean> implements EVPredicate {

    public EVSpatialCoveredBy(EVSpatialExpression left, EVSpatialExpression right) {
        super(left, right, new OperatorImpl<Geometry, Geometry, Boolean>() {

            @Override
            public Boolean apply(Geometry l, Geometry r) {
                return l.coveredBy(r);
            }
        });
    }

}
