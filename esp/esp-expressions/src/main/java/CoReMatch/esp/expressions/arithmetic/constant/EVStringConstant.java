package CoReMatch.esp.expressions.arithmetic.constant;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.event.Attribute.DataType;

public class EVStringConstant extends EVConstant<String> implements EVStringExpression {

    public EVStringConstant(String value) {
        super(value, DataType.STRING);
    }
}
