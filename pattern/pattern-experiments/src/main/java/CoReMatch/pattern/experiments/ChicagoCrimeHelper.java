package CoReMatch.pattern.experiments;

import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.expression.BooleanExpression;
import CoReMatch.esp.api.expression.arithmetic.atomic.IntConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.StringConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.Variable;
import CoReMatch.esp.api.expression.logical.True;
import CoReMatch.esp.api.expression.predicate.Equal;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.epa.pattern.regex.Sequence;
import CoReMatch.esp.api.epa.pattern.regex.Pattern;
import CoReMatch.esp.api.epa.pattern.regex.Atom;
import CoReMatch.esp.api.epa.pattern.regex.KleeneStar;
import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.esp.api.epa.Stream;
import java.util.ArrayList;
import java.util.List;
public class ChicagoCrimeHelper {

    public static Symbol createCrimeSymbolWithPrimaryType(char name, String primary) {

        BooleanExpression be = new Equal(new Variable("PRIMARY"), new StringConstant(primary));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);

    }

    public static PatternMatcher getCrimeSequenceQueryWithPrimaryTypes(List<String> primaryTypes, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String primary : primaryTypes) {
            ss.add(createCrimeSymbolWithPrimaryType(id++, primary));

        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();

        for (int i = 0; i < primaryTypes.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_BEAT");
            out.add(n + "_LAT");
            out.add(n + "_LON");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);

    }
}
