package CoReMatch.esp.bridges.nat.epa.impl.filter;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.Filter;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.esp.expressions.bool.EVBooleanExpression;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;

public class NativeFilter implements NativeOperator {

    private final EventSchema schema;

    private final EVBooleanExpression condition;

    private EPACallback callback;

    public NativeFilter(Filter def, EventChannel input) throws TranslatorException, IncompatibleTypeException {
        this.schema = def.computeOutputSchema(input.getSchema());
        this.condition = new ExpressionTranslator().translateBooleanExpression(def.getCondition(), input.getSchema(), new Bindings());

    }

    @Override
    public void process(EventChannel input, Event event) {
        if (condition.eval(event))
            callback.receive(event);
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
