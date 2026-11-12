package CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton;

import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.bindings.NoSuchBindingException;
import CoReMatch.event.Event;
import CoReMatch.event.impl.SimpleEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to assemble output events from a set of situations.
 * The values to retrieve as well as their positions inside the 
 * output-event are precomputed in order to efficiently build
 * those outputs.
 *
 */
public final class OutputMapping {

    /**
     * Remembers for each attribute the index of the 
     * corresponding aggregate
     */
    private int[] outputToBindingIdx;

    public OutputMapping(Symbols definitions, Output output) {
        this.outputToBindingIdx = new int[output.getSize()];
        List<Binding> bindings = new ArrayList<>();
        for (Symbol s : definitions) {
            s.getBindings().forEach(bindings::add);
        }

        int count = 0;
        for (String var : output) {
            for (int i = 0; i < bindings.size(); i++) {
                if (bindings.get(i).getName().equalsIgnoreCase(var))
                    outputToBindingIdx[count++] = i;
            }
        }
    }

    /**
     * Constructs an output event from the given set of situations
     * @param workingset the temporal configuration to create an output event for
     * @param currentTime the current stream time
     * @return the event to output
     * @throws NoSuchBindingException
     */
    public final Event create(BoundVariables variables, long currentTime) throws NoSuchBindingException {
        Object[] payload = new Object[outputToBindingIdx.length];
        for (int i = 0; i < outputToBindingIdx.length; i++) {
            payload[i] = variables.get(outputToBindingIdx[i]);
        }
        return new SimpleEvent(payload, currentTime, currentTime + 1);
    }

}
