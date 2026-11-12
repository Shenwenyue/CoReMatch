package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVIntVariable extends EVNumericVariable {

    public EVIntVariable(int index, boolean isBinding) {
        super(index, DataType.INTEGER, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Integer.class) : event.get(index, Integer.class);
    }
}
