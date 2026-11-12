package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.IntConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVIntConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class IntConstantTranslator extends Translator<IntConstant, EVIntConstant> {

    public IntConstantTranslator() {
        super(IntConstant.class, EVIntConstant.class);
    }

    @Override
    protected EVIntConstant process(IntConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVIntConstant(in.getValue());
    }

}
