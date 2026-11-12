package CoReMatch.esp.expressions.bool;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.event.Event;

import java.util.List;

public class EVFalse implements EVBooleanExpression {

    @Override
    public Boolean eval(Event event, BoundVariables bindings) {
        return false;
    }

    @Override
    public Boolean eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        return false;
    }
}
