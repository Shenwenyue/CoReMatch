package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

public class EVByteVariable extends EVNumericVariable {

    public EVByteVariable(int index, boolean isBinding) {
        super(index, DataType.BYTE, isBinding);
    }

    @Override
    public Number eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Byte.class) : event.get(index, Byte.class);
    }
}
