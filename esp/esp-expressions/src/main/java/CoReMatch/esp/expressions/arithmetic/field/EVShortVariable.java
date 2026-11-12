package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVShortVariable extends EVNumericVariable {

    public EVShortVariable(int index, boolean isBinding) {
        super(index, DataType.SHORT, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Short.class) : event.get(index, Short.class);
    }
}
