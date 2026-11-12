package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVNumericVariable extends EVVariable<Number> implements EVNumericExpression {

    public EVNumericVariable(int index, DataType type, boolean isBinding) {
        super(index, type, isBinding);
    }
}
