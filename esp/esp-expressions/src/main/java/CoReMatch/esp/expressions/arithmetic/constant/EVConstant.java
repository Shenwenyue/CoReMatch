package CoReMatch.esp.expressions.arithmetic.constant;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.arithmetic.EVArithmeticExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

import java.util.List;

public abstract class EVConstant<T> implements EVArithmeticExpression<T> {

    private final T value;

    private final DataType type;

    public EVConstant(T value, DataType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public T eval(Event event, BoundVariables bindings) {
        return value;
    }

    @Override
    public T eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        return value;
    }

    @Override
    public DataType getType() {
        return type;
    }
}
