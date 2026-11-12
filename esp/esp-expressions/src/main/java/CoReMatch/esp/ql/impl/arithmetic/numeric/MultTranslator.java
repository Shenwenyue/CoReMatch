package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Multiply;
import CoReMatch.esp.expressions.arithmetic.numeric.EVMult;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class MultTranslator extends Translator<Multiply, EVMult> {

    public MultTranslator() {
        super(Multiply.class, EVMult.class);
    }

    @Override
    protected EVMult process(Multiply in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVMult(
                translator.translateNumericExpression(in.getInput(0), schema, bindings),
                translator.translateNumericExpression(in.getInput(1), schema, bindings));
    }

}
