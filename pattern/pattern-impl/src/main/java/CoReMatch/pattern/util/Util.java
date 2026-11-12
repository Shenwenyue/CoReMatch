package CoReMatch.pattern.util;

import CoReMatch.db.DBRuntimeException;
import CoReMatch.db.core.secondaryindex.CompositeSecondaryTimeIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.pattern.cost.transform.SubPattern;
import CoReMatch.pattern.cost.transform.SubPatternCondition;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.epa.pattern.regex.*;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.BooleanExpression;
import CoReMatch.esp.api.expression.Predicate;
import CoReMatch.esp.api.expression.arithmetic.atomic.Variable;
import CoReMatch.esp.api.expression.logical.*;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.*;
import java.util.function.Function;

public class Util {

    public static TransformedPattern transformPattern(PatternMatcher src, EventSchema schema) {
        var sps = splitPattern(src.getPattern(), x -> new ArrayList<>(x));

        var result = new ArrayList<SubPattern>();

        int spIndex = 0;
        for (var subPattern : sps) {
            result.add(transformSubPattern(spIndex++, subPattern, src.getSymbols(), schema));

        }

        return new TransformedPattern(src, result);
    }

    @SuppressWarnings("unchecked")
    private static SubPattern transformSubPattern(int spIndex, List<Atom> sp, Symbols symbols, EventSchema schema) {

        List<SubPatternCondition<?>> result = new ArrayList<>();

        int pos = 0;

        boolean exact = true;
        for (Atom a : sp) {
            Symbol s = symbols.getSymbolById(a.getSymbol());
            var tr = PredicateToRange.translate(schema, s.getCondition());
            exact &= tr.isExact();
            int conditionIndex = 0;

            for (var r : tr.getRanges()) {
                result.add(new SubPatternCondition<>(a.getSymbol(), spIndex, pos, conditionIndex, (AttributeRange<? extends Number>) r));
                conditionIndex++;

            }
            pos++;
        }
        return new SubPattern(spIndex, sp.size(), exact, result);

    }

    public static <T> List<T> splitPattern(Pattern input, Function<List<Atom>, T> f) {
        List<T> result = new ArrayList<>();
        List<Atom> currentSequence = new ArrayList<>();
        splitPattern(input, currentSequence, result, f);
        if (!currentSequence.isEmpty()) {
            result.add(f.apply(currentSequence));
        }
        currentSequence.clear();
        return result;

    }

    private static <T> void splitPattern(Pattern input, List<Atom> currentSequence, List<T> result, Function<List<Atom>, T> f) {
        if (input instanceof Atom) {
            currentSequence.add((Atom) input);
        } else if (input instanceof KleenePlus) {
            KleenePlus kp = (KleenePlus) input;
            Pattern inner = kp.getInput();
            if (!(inner instanceof Atom))
                throw new IllegalArgumentException("Nested patterns not supported!");

            currentSequence.add((Atom) inner);
            splitPattern(new KleeneStar(inner), currentSequence, result, f);
        } else if (input instanceof KleeneStar) {
            if (!currentSequence.isEmpty()) {
                result.add(f.apply(currentSequence));
            }
            currentSequence.clear();
        } else if (input instanceof Sequence) {
            Sequence s = (Sequence) input;
            for (Pattern sp : s)
                splitPattern(sp, currentSequence, result, f);
        }
    }

    public static List<Symbol> reduceConditions(EventSchema schema, Symbols symbols) {
        List<Symbol> result = new ArrayList<>();

        for (Symbol orig : symbols) {
            result.add(new Symbol(orig.getId(), stripCondition(schema, orig.getCondition())));
        }
        return result;
    }

    public static BooleanExpression stripCondition(EventSchema schema, BooleanExpression in) {
        if (in instanceof And) {
            And and = (And) in;
            BooleanExpression left = stripCondition(schema, and.getInput(0));
            BooleanExpression right = stripCondition(schema, and.getInput(1));

            if (left instanceof True && !(right instanceof True))
                return right;
            else if (!(left instanceof True) && right instanceof True)
                return left;
            else if (left instanceof True && right instanceof True)
                return new True();
            else
                return new And(left, right);
        } else if (in instanceof Or) {
            Or or = (Or) in;
            BooleanExpression left = stripCondition(schema, or.getInput(0));
            BooleanExpression right = stripCondition(schema, or.getInput(1));

            if (left instanceof True || right instanceof True)
                return new True();
            else if (left instanceof False)
                return right;
            else if (right instanceof False)
                return left;
            else
                return new Or(left, right);
        } else if (in instanceof Not) {
            Not not = (Not) in;
            BooleanExpression child = stripCondition(schema, not.getInput(0));
            if (child instanceof True)
                return new False();
            else if (child instanceof False)
                return new True();
            else
                return new Not(child);
        } else if (in instanceof Predicate) {
            Predicate pred = (Predicate) in;
            return stripConditionPredicate(schema, pred);
        } else if (in instanceof True || in instanceof False)
            return in;
        else
            throw new DBRuntimeException("Unknown Boolean expression: " + in);
    }

    private static BooleanExpression stripConditionPredicate(EventSchema schema, Predicate p) {
        Set<Variable> vars = new HashSet<>();
        for (int i = 0; i < p.getArity(); i++) {
            extractVariables(vars, p.getInput(i));
        }

        try {
            for (Variable v : vars)
                schema.byName(v.getName());
            return p;
        } catch (SchemaException se) {
            return new True();
        }
    }

    private static void extractVariables(Set<Variable> akkum, ArithmeticExpression ai) {
        if (ai instanceof Variable) {
            akkum.add((Variable) ai);
        } else {
            for (int i = 0; i < ai.getArity(); i++) {
                extractVariables(akkum, ai.getInput(i));
            }
        }
    }

    public static <T extends SecondaryTimeIndex<?>> T getIndexForRange(AttributeRange<?> r1, List<T> idxs) {
        for (var idx : idxs) {
            if (idx.getAttributeName().equals(r1.getName()))
                return idx;
        }
        throw new NoSuchElementException("No index found for attribute " + r1.getName());
    }

    @SuppressWarnings("unchecked")
    public static <K1 extends Comparable<K1>, K2 extends Comparable<K2>>
            CompositeSecondaryTimeIndex<K1, K2> getCompositeIndexForAttributes(
                    String attr1, String attr2,
                    List<? extends CompositeSecondaryTimeIndex<?, ?>> compositeIdxs) {
        for (var idx : compositeIdxs) {
            if (idx.getAttribute1Name().equalsIgnoreCase(attr1) &&
                idx.getAttribute2Name().equalsIgnoreCase(attr2)) {
                return (CompositeSecondaryTimeIndex<K1, K2>) idx;
            }
        }
        throw new NoSuchElementException("No composite index found for attributes (" + attr1 + ", " + attr2 + ")");
    }

    public static boolean hasCompositeIndexForAttributes(
            String attr1, String attr2,
            List<? extends CompositeSecondaryTimeIndex<?, ?>> compositeIdxs) {
        for (var idx : compositeIdxs) {
            if (idx.getAttribute1Name().equalsIgnoreCase(attr1) &&
                idx.getAttribute2Name().equalsIgnoreCase(attr2)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static CompositeSecondaryTimeIndex<?, ?> getCompositeIndexForCondition(
            TransformedPattern.ExecutableCondition condition,
            List<? extends SecondaryTimeIndex<?>> idxs) {
        if (!condition.isComposite()) {
            throw new IllegalArgumentException("Condition is not a composite condition");
        }

        var compositeRange = condition.getCompositeRange();
        String attr1 = compositeRange.getAttribute1Name();
        String attr2 = compositeRange.getAttribute2Name();

        for (var idx : idxs) {
            if (idx instanceof CompositeSecondaryTimeIndex) {
                var compositeIdx = (CompositeSecondaryTimeIndex<?, ?>) idx;
                if (compositeIdx.getAttribute1Name().equalsIgnoreCase(attr1) &&
                    compositeIdx.getAttribute2Name().equalsIgnoreCase(attr2)) {
                    return compositeIdx;
                }
            }
        }
        throw new NoSuchElementException("No composite index found for attributes (" + attr1 + ", " + attr2 + ")");
    }

    @SuppressWarnings("unchecked")
    public static <T extends SecondaryTimeIndex<?>> T getIndependentIndexForAttribute(
            String attrName, List<T> idxs) {
        for (var idx : idxs) {

            if (!(idx instanceof CompositeSecondaryTimeIndex) &&
                idx.getAttributeName().equalsIgnoreCase(attrName)) {
                return idx;
            }
        }
        throw new NoSuchElementException("No independent index found for attribute " + attrName);
    }

}
