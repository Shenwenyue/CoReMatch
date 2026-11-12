package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Tan;
import CoReMatch.esp.expressions.arithmetic.numeric.EVTan;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class TanTranslator extends Translator<Tan, EVTan> {

    public TanTranslator() {
        super(Tan.class, EVTan.class);
    }

    @Override
    protected EVTan process(Tan in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVTan(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
