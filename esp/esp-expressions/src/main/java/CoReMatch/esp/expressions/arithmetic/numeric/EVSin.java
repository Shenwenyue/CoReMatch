package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVSin extends EVUnaryNumericExpression {

    public EVSin(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.sin(x.doubleValue()));
    }
}
