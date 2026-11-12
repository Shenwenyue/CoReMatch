package CoReMatch.esp.bridges.nat.epa.impl.projection;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.Projection;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.esp.expressions.arithmetic.EVArithmeticExpression;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;
import CoReMatch.event.impl.SimpleEvent;

public class NativeProjection implements NativeOperator {

    private final EventSchema schema;

    private final EVArithmeticExpression<?>[] expressions;

    private EPACallback callback;

    public NativeProjection(Projection def, EventChannel input) throws TranslatorException, IncompatibleTypeException, SchemaException {
        this.schema = def.computeOutputSchema(input.getSchema());
        this.expressions = new EVArithmeticExpression<?>[def.getProjections().length];
        ExpressionTranslator et = new ExpressionTranslator();
        Bindings b = new Bindings();
        for (int i = 0; i < this.expressions.length; i++) {
            switch (schema.getAttribute(i).getType()) {
                case BYTE:
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    this.expressions[i] = et.translateNumericExpression(def.getProjections()[i].getExpression(), input.getSchema(), b);
                    break;
                case STRING:
                    this.expressions[i] = et.translateStringExpression(def.getProjections()[i].getExpression(), input.getSchema(), b);
                    break;
                case GEOMETRY:
                    this.expressions[i] = et.translateSpatialExpression(def.getProjections()[i].getExpression(), input.getSchema(), b);
                    break;
                default:
                    throw new IncompatibleTypeException("Don't know how to translate expression of type: " + schema.getAttribute(i).getType());
            }
        }
    }

    @Override
    public void process(EventChannel input, Event event) {
        Object[] payload = new Object[expressions.length];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = expressions[i].eval(event);
        }
        callback.receive(new SimpleEvent(payload, event.getT1(), event.getT2()));
    }

    @Override
    public void setCallback(EPACallback callback) {
        this.callback = callback;
    }

    @Override
    public EventSchema getSchema() {
        return schema;
    }

    @Override
    public void flushState() {
    }

}
