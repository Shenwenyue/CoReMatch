package CoReMatch.esp.ql.impl.bool;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.logical.Or;
import CoReMatch.esp.expressions.bool.EVBooleanExpression;
import CoReMatch.esp.expressions.bool.EVOr;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class OrTranslator extends Translator<Or, EVOr> {

    public OrTranslator() {
        super(Or.class, EVOr.class);
    }

    @Override
    protected EVOr process(Or in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        EVBooleanExpression left = translator.translateBooleanExpression(in.getInput(0), schema, bindings);
        EVBooleanExpression right = translator.translateBooleanExpression(in.getInput(1), schema, bindings);
        return new EVOr(left, right);
    }
}
