package CoReMatch.esp.expressions.bool;

import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;

public class EVNot extends EVAbstractUnaryExpression<Boolean, Boolean> implements EVBooleanExpression {

    public EVNot(EVBooleanExpression input) {
        super(input, new OperatorImpl<Boolean, Boolean>() {

            @Override
            public Boolean apply(Boolean in) {
                return !in;
            }
        });
    }


}
