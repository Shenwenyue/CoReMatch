package CoReMatch.esp.expressions.arithmetic.field;

import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.arithmetic.EVSetExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EVSetVariable extends EVVariable<ArrayList<Object>> implements EVSetExpression {

    public EVSetVariable(int index, boolean isBinding) {
        super(index, DataType.SET, isBinding);
    }

    @Override
    public ArrayList<Object> eval(Event event, BoundVariables bindings) {
        Object o = event.get(index);
        ArrayList<Object> result = new ArrayList<>();
        if (o instanceof ArrayList) {
            for (Object elem : (ArrayList<?>) o)
                result.add(elem);
        } else {
            result.add(o);
        }
        return result;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public ArrayList<Object> eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        return eval(event, bindings);
    }

}
