package CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.bridge.EPACallback;
import CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton.BufferAutomaton.Run;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Event;

import java.util.*;

public class AutomatonPatternMatcher {

    private final BufferAutomaton automaton;

    Queue<Run> resultRuns = new ArrayDeque<>();

    List<Run> runs = new LinkedList<>();

    CountRusult countRusult = new CountRusult();

    private EPACallback callback;

    public AutomatonPatternMatcher(BufferAutomaton automaton) {
        this.automaton = automaton;
    }

    public void match(Event event) {

        if (automaton.startsRun(event)) {
            runs.add(automaton.newRun());
        }

        for (Iterator<Run> ri = runs.iterator(); ri.hasNext(); ) {
            Run r = ri.next();

            if (r.isTimedOut(event.getT1()) || !r.update(event)) {
                ri.remove();
            } else if (r.isAccept()) {
                resultRuns.add(r);

                ri.remove();
            }
        }
        while (!resultRuns.isEmpty()) {
            countRusult.addNumber();
            Run result = resultRuns.poll();

        }

    }

    public void setCallback(EPACallback callback) {
        this.callback = callback;
    }
}
