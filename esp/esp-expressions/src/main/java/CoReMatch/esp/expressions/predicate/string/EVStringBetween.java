package CoReMatch.esp.expressions.predicate.string;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractTrinaryExpression;

public class EVStringBetween extends EVAbstractTrinaryExpression<String, String, String, Boolean> implements EVPredicate {

    public EVStringBetween(EVStringExpression value, EVStringExpression lower, EVStringExpression upper) {
        super(value, lower, upper, new OperatorImpl<String, String, String, Boolean>() {

            @Override
            public Boolean apply(String v, String l, String u) {
                return l.compareTo(v) <= 0 && u.compareTo(v) >= 0;
            }
        });
    }

}
