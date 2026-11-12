package CoReMatch.esp.expressions.arithmetic.constant;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVNumberConstant extends EVConstant<Number> implements EVNumericExpression {

    public EVNumberConstant(Number value) {
        super(value, DataType.fromValue(value));
    }
}
