package CoReMatch.esp.expressions.spatial.geometric;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.util.EVAbstractNAryExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;

import java.util.Collections;
import java.util.List;

public class EVSpatialGeometryCollection extends EVAbstractNAryExpression<Geometry, Geometry> implements EVSpatialExpression {

    public EVSpatialGeometryCollection(EvaluableExpression<Geometry>[] inputs) {
        super((Geometry[] x) -> new GeometryCollection(x, new GeometryFactory()), inputs);
    }

    @Override
    public DataType getType() {
        return DataType.GEOMETRY;
    }

    @Override
    public Geometry eval(Event event, BoundVariables bindings) {
        return eval(Collections.emptyList(), event, bindings);
    }

    @Override
    public Geometry eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        EvaluableExpression<Geometry>[] inputs = getInputs();
        Geometry[] ev = new Geometry[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            EvaluableExpression<Geometry> input = inputs[i];
            ev[i] = input.eval(event, bindings);
        }
        return getImpl().apply(ev);
    }
}
