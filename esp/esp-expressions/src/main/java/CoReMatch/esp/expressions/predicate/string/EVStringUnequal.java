package CoReMatch.esp.expressions.predicate.string;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;

public class EVStringUnequal extends EVAbstractBinaryExpression<String, String, Boolean> implements EVPredicate {

    public EVStringUnequal(EVStringExpression left, EVStringExpression right) {
        super(left, right, new OperatorImpl<String, String, Boolean>() {

            @Override
            public Boolean apply(String l, String r) {
                return l.compareTo(r) != 0;
            }
        });
    }

}
