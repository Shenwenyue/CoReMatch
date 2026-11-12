package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVStringVariable extends EVVariable<String> implements EVStringExpression {

    public EVStringVariable(int index, boolean isBinding) {
        super(index, DataType.STRING, isBinding);
    }

    @Override
    public String eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, String.class) : event.get(index, String.class);
    }
}
