package CoReMatch.esp.bridges.nat.epa.impl.pattern.automaton;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.bindings.BoundVariables;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;

import java.sql.SQLOutput;
import java.util.*;

public class BufferAutomaton {
    private final PatternMatcher pm;

    private final Map<Character, EVSymbol> translatedSymbols;

    private final int numberOfBindings;

    private final OutputMapping outputMapping;

    private final BufferState initialState;

    public BufferAutomaton(PatternMatcher def, EventSchema inputSchema) throws TranslatorException, IncompatibleTypeException {

        this.pm = def;

        this.translatedSymbols = new HashMap<>();

        List<Binding> previousBindings = new ArrayList<>();
        for (Symbol s : pm.getSymbols()) {
            Bindings tmp = new Bindings(previousBindings.toArray(new Binding[previousBindings.size()]));
            EVSymbol evs = EVSymbol.create(s, inputSchema, tmp);

            translatedSymbols.put(evs.getId(), evs);

            for (Binding b : s.getBindings())
                previousBindings.add(b);

        }

        this.numberOfBindings = previousBindings.size();
        this.outputMapping = new OutputMapping(pm.getSymbols(), pm.getOutput());

        {
            Automaton automaton = new RegExp(def.getPattern().toString()).toAutomaton();
            Map<State, BufferState> stateMap = new HashMap<>();
            State initState = automaton.getInitialState();
            stateMap.put(initState, new BufferState(true, false));

            for (State s : automaton.getStates()) {

                if (!stateMap.containsKey(s))
                    stateMap.put(s, new BufferState(false, s.isAccept()));
                Set<dk.brics.automaton.Transition> t = s.getTransitions();
                for (dk.brics.automaton.Transition transition : t) {

                    if (!stateMap.containsKey(transition.getDest())) {
                        stateMap.put(transition.getDest(), new BufferState(false, transition.getDest().isAccept()));
                    }
                    Transition.Type type = (s == transition.getDest()) ? Transition.Type.TAKE : Transition.Type.PROCEED;
                    stateMap.get(s).addTransition(
                            new Transition(translatedSymbols.get(transition.getMax()),
                                    stateMap.get(transition.getDest()), type));
                }
            }
            this.initialState = stateMap.get(initState);
        }
    }

    public boolean startsRun(Event e) {
        for (Transition t : initialState.transitions) {
            if (t.symbol.getCondition().eval(e))
                return true;
        }
        return false;
    }

    public Run newRun() {
        return new Run();
    }

    static class BufferState {

        List<Transition> transitions;

        private boolean accept;

        public BufferState(boolean initial, boolean accept) {
            transitions = new ArrayList<>();
            this.accept = accept;
        }

        public boolean isAccept() {
            return accept;
        }

        protected void addTransition(Transition transition) {
            transitions.add(transition);
            Collections.sort(transitions);
        }
    }

    static class Transition implements Comparable<Transition> {

        final EVSymbol symbol;

        ;
        final BufferState target;
        final Type type;
        Transition(EVSymbol symbol, BufferState target, Type type) {
            this.symbol = symbol;
            this.target = target;
            this.type = type;
        }

        @Override
        public int compareTo(Transition o) {
            return type.compareTo(o.type);
        }

        static enum Type {PROCEED, TAKE}
    }

    public class Run {

        private List<Event> events;

        private BoundVariables variables;

        private BufferState currentState;

        public Run() {
            events = new ArrayList<>();
            variables = new BoundVariables(numberOfBindings);
            currentState = initialState;
        }

        public boolean update(Event e) {

            for (Transition t : currentState.transitions) {
                if (t.symbol.getCondition().eval(events, e, variables)) {
                    t.symbol.updateBindings(events, e, variables);
                    currentState = t.target;
                    events.add(e);
                    return true;

                }
            }
            return false;
        }

        public String toString(int t) {
            return "Matched " + events.size() + " events in result " + t;
        }

        public boolean isAccept() {
            return currentState.isAccept();
        }

        public boolean isTimedOut(long ts) {
            return !events.isEmpty() && ts - events.get(0).getT1() >= pm.getWithin();
        }

        public Event createResult() {
            return outputMapping.create(variables, events.get(events.size() - 1).getT1());
        }
    }

}
