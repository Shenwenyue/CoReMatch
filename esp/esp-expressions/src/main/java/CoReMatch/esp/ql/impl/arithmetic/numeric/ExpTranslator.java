package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Exp;
import CoReMatch.esp.expressions.arithmetic.numeric.EVExp;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ExpTranslator extends Translator<Exp, EVExp> {

    public ExpTranslator() {
        super(Exp.class, EVExp.class);
    }

    @Override
    protected EVExp process(Exp in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVExp(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
