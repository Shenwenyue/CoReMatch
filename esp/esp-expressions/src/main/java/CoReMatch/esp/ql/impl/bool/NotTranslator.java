package CoReMatch.esp.ql.impl.bool;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.logical.Not;
import CoReMatch.esp.expressions.bool.EVBooleanExpression;
import CoReMatch.esp.expressions.bool.EVNot;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class NotTranslator extends Translator<Not, EVNot> {

    public NotTranslator() {
        super(Not.class, EVNot.class);
    }

    @Override
    protected EVNot process(Not in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        EVBooleanExpression evin = translator.translateBooleanExpression(in.getInput(0), schema, bindings);
        return new EVNot(evin);
    }
}
