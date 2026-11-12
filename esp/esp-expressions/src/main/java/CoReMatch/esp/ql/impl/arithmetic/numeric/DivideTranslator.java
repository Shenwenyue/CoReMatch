package CoReMatch.esp.ql.impl.arithmetic.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.compund.numeric.Divide;
import CoReMatch.esp.expressions.arithmetic.numeric.EVDivide;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class DivideTranslator extends Translator<Divide, EVDivide> {

    public DivideTranslator() {
        super(Divide.class, EVDivide.class);
    }

    @Override
    protected EVDivide process(Divide in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVDivide(
                translator.translateNumericExpression(in.getInput(0), schema, bindings),
                translator.translateNumericExpression(in.getInput(1), schema, bindings));
    }

}
