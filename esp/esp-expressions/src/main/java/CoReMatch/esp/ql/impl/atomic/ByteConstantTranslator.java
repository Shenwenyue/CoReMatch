package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.ByteConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVByteConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class ByteConstantTranslator extends Translator<ByteConstant, EVByteConstant> {

    public ByteConstantTranslator() {
        super(ByteConstant.class, EVByteConstant.class);
    }

    @Override
    protected EVByteConstant process(ByteConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVByteConstant(in.getValue());
    }

}
