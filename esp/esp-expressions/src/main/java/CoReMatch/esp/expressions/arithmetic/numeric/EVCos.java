package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVCos extends EVUnaryNumericExpression {

    public EVCos(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.cos(x.doubleValue()));
    }
}
