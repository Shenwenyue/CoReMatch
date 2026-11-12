package CoReMatch.esp.expressions.arithmetic.field;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

import java.util.List;

public class EVSpatialVariable extends EVVariable<Geometry> implements EVSpatialExpression {

    public EVSpatialVariable(int index, boolean isBinding) {
        super(index, DataType.GEOMETRY, isBinding);
    }

    @Override
    public Geometry eval(Event event, BoundVariables bindings) {
        return isBinding ? bindings.get(index, Geometry.class) : event.get(index, Geometry.class);
    }

    @Override
    public Geometry eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        return eval(event, bindings);
    }

}
