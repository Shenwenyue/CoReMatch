package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.ATan;
import CoReMatch.esp.expressions.arithmetic.numeric.EVATan;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ATanTranslator extends Translator<ATan, EVATan> {

    public ATanTranslator() {
        super(ATan.class, EVATan.class);
    }

    @Override
    protected EVATan process(ATan in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVATan(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
