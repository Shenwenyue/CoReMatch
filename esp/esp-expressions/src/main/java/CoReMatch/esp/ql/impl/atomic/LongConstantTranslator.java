package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.LongConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVLongConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class LongConstantTranslator extends Translator<LongConstant, EVLongConstant> {

    public LongConstantTranslator() {
        super(LongConstant.class, EVLongConstant.class);
    }

    @Override
    protected EVLongConstant process(LongConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVLongConstant(in.getValue());
    }

}
