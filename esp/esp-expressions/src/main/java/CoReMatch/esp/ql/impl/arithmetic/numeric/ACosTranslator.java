package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.ACos;
import CoReMatch.esp.expressions.arithmetic.numeric.EVACos;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ACosTranslator extends Translator<ACos, EVACos> {

    public ACosTranslator() {
        super(ACos.class, EVACos.class);
    }

    @Override
    protected EVACos process(ACos in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVACos(translator.translateNumericExpression(in.getInput(0), schema, bindings));
    }

}
