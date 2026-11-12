package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.event.Attribute;

public class EVStringPrevVariable extends EVPrevVariable<String> implements EVStringExpression {

    public EVStringPrevVariable(int index, Attribute.DataType type, int prev, EVVariable<String> variable) {
        super(index, type, prev, variable);
    }

}
