package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.Distance;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialDistance;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class DistanceTranslator extends Translator<Distance, EVSpatialDistance> {

    public DistanceTranslator() {
        super(Distance.class, EVSpatialDistance.class);
    }

    @Override
    protected EVSpatialDistance process(Distance in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialDistance(
                translator.translateSpatialExpression(in.getInput(0), schema, bindings),
                translator.translateSpatialExpression(in.getInput(1), schema, bindings));
    }

}
