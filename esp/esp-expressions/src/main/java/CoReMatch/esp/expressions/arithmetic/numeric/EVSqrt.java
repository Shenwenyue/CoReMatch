package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVSqrt extends EVUnaryNumericExpression {

    public EVSqrt(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.sqrt(x.doubleValue()));
    }
}
