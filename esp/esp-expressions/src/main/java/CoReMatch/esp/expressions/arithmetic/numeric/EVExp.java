package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVExp extends EVUnaryNumericExpression {

    public EVExp(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.exp(x.doubleValue()));
    }
}
