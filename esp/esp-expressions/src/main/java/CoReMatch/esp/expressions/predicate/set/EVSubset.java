package CoReMatch.esp.expressions.predicate.set;

import CoReMatch.esp.expressions.arithmetic.EVSetExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;

import java.util.ArrayList;

public class EVSubset extends EVAbstractBinaryExpression<ArrayList<Object>, ArrayList<Object>, Boolean> implements EVPredicate {

    public EVSubset(EVSetExpression left, EVSetExpression right) {
        super(left, right, new OperatorImpl<ArrayList<Object>, ArrayList<Object>, Boolean>() {

            @Override
            public Boolean apply(ArrayList<Object> l, ArrayList<Object> r) {
                return r.containsAll(l);
            }
        });
    }

}
