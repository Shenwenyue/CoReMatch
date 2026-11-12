package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.Centroid;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialCentroid;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class CentroidTranslator extends Translator<Centroid, EVSpatialCentroid> {

    public CentroidTranslator() {
        super(Centroid.class, EVSpatialCentroid.class);
    }

    @Override
    protected EVSpatialCentroid process(Centroid in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialCentroid(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
