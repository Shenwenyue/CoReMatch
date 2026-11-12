package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.StringConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVStringConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class StringConstantTranslator extends Translator<StringConstant, EVStringConstant> {

    public StringConstantTranslator() {
        super(StringConstant.class, EVStringConstant.class);
    }

    @Override
    protected EVStringConstant process(StringConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVStringConstant(in.getValue());
    }

}
