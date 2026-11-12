package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.DoubleConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVDoubleConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class DoubleConstantTranslator extends Translator<DoubleConstant, EVDoubleConstant> {

    public DoubleConstantTranslator() {
        super(DoubleConstant.class, EVDoubleConstant.class);
    }

    @Override
    protected EVDoubleConstant process(DoubleConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVDoubleConstant(in.getValue());
    }

}
