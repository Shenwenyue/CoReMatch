package CoReMatch.esp.api.expression.predicate.spatial;

import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.base.AbstractExpression;

/**
 * Checks if the left input is within distance from the right one
 */
public class IsWithinDistance extends AbstractExpression<ArithmeticExpression> implements Predicate {

    private final double distance;

    /**
     * @param distance the distance
     * @param left     the first input
     * @param right    the second input
     */
    public IsWithinDistance(double distance, ArithmeticExpression left, ArithmeticExpression right) {
        super(left, right);
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "(" + getInput(0) + ") is within " + distance + " distance from (" + getInput(1) + ")";
    }

}
