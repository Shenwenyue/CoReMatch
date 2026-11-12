package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.LatLonDistance;
import CoReMatch.esp.expressions.spatial.numeric.EVLatLonDistance;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class LatLonDistanceTranslator extends Translator<LatLonDistance, EVLatLonDistance> {

    public LatLonDistanceTranslator() {
        super(LatLonDistance.class, EVLatLonDistance.class);
    }

    @Override
    protected EVLatLonDistance process(LatLonDistance in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVLatLonDistance(
                translator.translateSpatialExpression(in.getInput(0), schema, bindings),
                translator.translateSpatialExpression(in.getInput(1), schema, bindings));
    }

}
