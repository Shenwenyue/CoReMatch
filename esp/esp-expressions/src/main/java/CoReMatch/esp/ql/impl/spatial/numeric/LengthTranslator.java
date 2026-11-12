package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.Length;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialLength;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class LengthTranslator extends Translator<Length, EVSpatialLength> {

    public LengthTranslator() {
        super(Length.class, EVSpatialLength.class);
    }

    @Override
    protected EVSpatialLength process(Length in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialLength(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
