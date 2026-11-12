package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVASin extends EVUnaryNumericExpression {

    public EVASin(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.asin(x.doubleValue()));
    }
}
