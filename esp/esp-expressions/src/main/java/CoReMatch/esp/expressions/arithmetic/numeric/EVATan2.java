package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public class EVATan2 extends EVBinaryNumericExpression {

    public EVATan2(EVNumericExpression y, EVNumericExpression x) {
        super(y, x, DataType.DOUBLE, (Number y1, Number x1) -> Math.atan2(y1.doubleValue(), x1.doubleValue()));
    }
}
