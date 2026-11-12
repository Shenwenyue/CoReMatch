package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVATan extends EVUnaryNumericExpression {

    public EVATan(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.atan(x.doubleValue()));
    }
}
