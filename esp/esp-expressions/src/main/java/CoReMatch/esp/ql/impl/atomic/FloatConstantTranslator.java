package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.FloatConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVFloatConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class FloatConstantTranslator extends Translator<FloatConstant, EVFloatConstant> {

    public FloatConstantTranslator() {
        super(FloatConstant.class, EVFloatConstant.class);
    }

    @Override
    protected EVFloatConstant process(FloatConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVFloatConstant(in.getValue());
    }

}
