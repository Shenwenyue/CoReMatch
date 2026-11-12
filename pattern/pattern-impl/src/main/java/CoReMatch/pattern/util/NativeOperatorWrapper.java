package CoReMatch.pattern.util;

import CoReMatch.common.EPException;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.EPA;
import CoReMatch.esp.api.epa.UserDefinedEPA;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class NativeOperatorWrapper extends UserDefinedEPA {

    /**
     * The serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private final OperatorFactory factory;
    private NativeOperator op;

    public NativeOperatorWrapper(OperatorFactory f) {
        this.factory = f;
    }

    @Override
    public EventSchema computeOutputSchema(EventSchema... inputSchemas)
            throws SchemaException, IncompatibleTypeException {
        return this.factory.getDefinition().computeOutputSchema(inputSchemas);
    }

    @Override
    public void initialize(EventChannel... inputStreams) throws EPException {

        this.op = this.factory.create(inputStreams);
        this.op.setCallback(NativeOperatorWrapper.this::publishResult);
    }

    @Override
    public void process(EventChannel input, Event event) {
        op.process(input, event);
    }

    @Override
    public void destroy(boolean flush) throws EPException {
        if (flush)
            op.flushState();
    }

    public static interface OperatorFactory {
        EPA getDefinition();

        NativeOperator create(EventChannel... inputSchemas);
    }

}
