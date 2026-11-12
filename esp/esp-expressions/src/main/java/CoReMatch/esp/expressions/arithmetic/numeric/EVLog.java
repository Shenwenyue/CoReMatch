package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVLog extends EVBinaryNumericExpression {

    public EVLog(EVNumericExpression left, EVNumericExpression right) {
        super(left, right, DataType.DOUBLE, (Number x, Number y) -> Math.log(x.doubleValue()) / Math.log(y.doubleValue()));
    }
}
