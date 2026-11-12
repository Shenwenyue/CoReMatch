package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Sin;
import CoReMatch.esp.expressions.arithmetic.numeric.EVSin;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class SinTranslator extends Translator<Sin, EVSin> {

    public SinTranslator() {
        super(Sin.class, EVSin.class);
    }

    @Override
    protected EVSin process(Sin in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSin(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
