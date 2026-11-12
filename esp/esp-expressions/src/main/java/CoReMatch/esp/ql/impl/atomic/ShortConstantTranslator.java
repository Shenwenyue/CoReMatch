package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.ShortConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVShortConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class ShortConstantTranslator extends Translator<ShortConstant, EVShortConstant> {

    public ShortConstantTranslator() {
        super(ShortConstant.class, EVShortConstant.class);
    }

    @Override
    protected EVShortConstant process(ShortConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVShortConstant(in.getValue());
    }

}
