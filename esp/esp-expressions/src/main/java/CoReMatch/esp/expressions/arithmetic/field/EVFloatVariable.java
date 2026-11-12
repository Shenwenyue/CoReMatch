package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVFloatVariable extends EVNumericVariable {

    public EVFloatVariable(int index, boolean isBinding) {
        super(index, DataType.FLOAT, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Float.class) : event.get(index, Float.class);
    }
}
