package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVDivide extends EVBinaryNumericExpression {

    public EVDivide(EVNumericExpression left, EVNumericExpression right) {
        super(left, right, DataType.DOUBLE, (Number x, Number y) -> x.doubleValue() / y.doubleValue());
    }
}
