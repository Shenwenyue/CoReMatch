package CoReMatch.esp.ql.impl.bool;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.logical.True;
import CoReMatch.esp.expressions.bool.EVTrue;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class TrueTranslator extends Translator<True, EVTrue> {

    public TrueTranslator() {
        super(True.class, EVTrue.class);
    }

    @Override
    protected EVTrue process(True in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) {
        return new EVTrue();
    }
}
