package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.ASin;
import CoReMatch.esp.expressions.arithmetic.numeric.EVASin;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ASinTranslator extends Translator<ASin, EVASin> {

    public ASinTranslator() {
        super(ASin.class, EVASin.class);
    }

    @Override
    protected EVASin process(ASin in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVASin(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
