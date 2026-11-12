package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.event.Attribute;

public class EVNumericPrevVariable extends EVPrevVariable<Number> implements EVNumericExpression {

    public EVNumericPrevVariable(int index, Attribute.DataType type, int prev, EVVariable<Number> variable) {
        super(index, type, prev, variable);
    }
}
