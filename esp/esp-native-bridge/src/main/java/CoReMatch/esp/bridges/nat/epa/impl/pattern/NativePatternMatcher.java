package CoReMatch.esp.bridges.nat.epa.impl.pattern;

import CoReMatch.common.EPRuntimeException;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.util.EventPartitioner;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;
import CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton.AutomatonPatternMatcher;
import CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton.BufferAutomaton;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.HashMap;
import java.util.Map;

public class NativePatternMatcher implements NativeOperator {

    private final Map<Object, AutomatonPatternMatcher> partitions = new HashMap<>();

    private final BufferAutomaton automaton;

    private final EventSchema schemaOut;
    private final EventSchema schemaIn;

    private EPACallback operatorCallback;
    private EPACallback automatonCallback = new EPACallback() {

        @Override
        public void receive(Event event) {
            if (operatorCallback != null)
                operatorCallback.receive(event);
        }
    };

    private EventPartitioner partitioner;

    public NativePatternMatcher(PatternMatcher def, EventChannel input) throws SchemaException, IncompatibleTypeException, TranslatorException {

        this.schemaOut = def.computeOutputSchema(input.getSchema());
        this.schemaIn = input.getSchema();
        this.automaton = new BufferAutomaton(def, schemaIn);
        this.partitioner = new EventPartitioner(input.getSchema(), def.getPartitionBy());
    }

    private AutomatonPatternMatcher getPatternMatcher(Object key) {
        AutomatonPatternMatcher result = partitions.get(key);
        if (result == null) {
            result = new AutomatonPatternMatcher(automaton);
            result.setCallback(automatonCallback);
            partitions.put(key, result);
        }
        return result;
    }

    @Override
    public void process(EventChannel input, Event event) {
        getPatternMatcher(partitioner.buildKey(event)).match(event);
    }

    @Override
    public void setCallback(EPACallback callback) {
        this.operatorCallback = callback;
    }

    @Override
    public EventSchema getSchema() {
        return schemaOut;
    }

    @Override
    public void flushState() {
    }

    public void clearState() {
        partitions.clear();
    }

}
