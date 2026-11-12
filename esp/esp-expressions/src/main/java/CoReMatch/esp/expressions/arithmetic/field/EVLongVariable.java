package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVLongVariable extends EVNumericVariable {

    public EVLongVariable(int index, boolean isBinding) {
        super(index, DataType.LONG, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Long.class) : event.get(index, Long.class);
    }
}
