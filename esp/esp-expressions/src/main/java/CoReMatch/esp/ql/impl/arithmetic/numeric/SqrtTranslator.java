package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Sqrt;
import CoReMatch.esp.expressions.arithmetic.numeric.EVSqrt;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class SqrtTranslator extends Translator<Sqrt, EVSqrt> {

    public SqrtTranslator() {
        super(Sqrt.class, EVSqrt.class);
    }

    @Override
    protected EVSqrt process(Sqrt in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSqrt(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
