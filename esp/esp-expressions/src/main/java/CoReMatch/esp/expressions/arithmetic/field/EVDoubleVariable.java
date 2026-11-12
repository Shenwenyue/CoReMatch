package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVDoubleVariable extends EVNumericVariable {

    public EVDoubleVariable(int index, boolean isBinding) {
        super(index, DataType.DOUBLE, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Double.class) : event.get(index, Double.class);
    }
}
