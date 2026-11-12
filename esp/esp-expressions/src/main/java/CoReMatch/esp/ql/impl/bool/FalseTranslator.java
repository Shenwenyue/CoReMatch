package CoReMatch.esp.ql.impl.bool;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.logical.False;
import CoReMatch.esp.expressions.bool.EVFalse;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class FalseTranslator extends Translator<False, EVFalse> {

    public FalseTranslator() {
        super(False.class, EVFalse.class);
    }

    @Override
    protected EVFalse process(False in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) {
        return new EVFalse();
    }
}
