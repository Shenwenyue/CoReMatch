package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVACos extends EVUnaryNumericExpression {

    public EVACos(EVNumericExpression input) {
        super(input, DataType.DOUBLE, (Number x) -> Math.acos(x.doubleValue()));
    }
}
