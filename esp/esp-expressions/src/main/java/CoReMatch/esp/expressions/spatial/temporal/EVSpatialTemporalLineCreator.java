package CoReMatch.esp.expressions.spatial.temporal;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.esp.api.expression.spatial.temporal.TemporalGeometryFactory;
import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.event.Attribute;
import CoReMatch.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EVSpatialTemporalLineCreator implements EVSpatialExpression {

    private final EVSpatialExpression input;

    public EVSpatialTemporalLineCreator(EVSpatialExpression input) {
        this.input = input;
    }

    @Override
    public Attribute.DataType getType() {
        return Attribute.DataType.GEOMETRY;
    }

    @Override
    public Geometry eval(Event event, BoundVariables bindings) {
        return eval(Collections.emptyList(), event, bindings);
    }

    //TODO: This previousEvents thing feels like a dirty hack.
    @Override
    public Geometry eval(List<Event> previousEvents, Event event, BoundVariables bindings) {
        TemporalGeometryFactory geometryFactory = new TemporalGeometryFactory();
        List<Coordinate> results = new ArrayList<>();
        long[] timestamps = new long[previousEvents.size() + 1];
        int i = 0;
        for (Event previousEvent : previousEvents) {
            results.addAll(Arrays.asList(input.eval(previousEvent, bindings).getCoordinates()));
            timestamps[i++] = previousEvent.getT1();
        }
        results.addAll(Arrays.asList(input.eval(event, bindings).getCoordinates()));
        timestamps[i++] = event.getT1();
        return geometryFactory.createTemporalLineString(results.toArray(new Coordinate[results.size()]), timestamps);
    }
}
