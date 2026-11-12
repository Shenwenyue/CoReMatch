package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVTan extends EVUnaryNumericExpression {

    public EVTan(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.tan(x.doubleValue()));
    }
}
