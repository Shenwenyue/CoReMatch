package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.GeometryCollection;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialGeometryCollection;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class GeometryCollectionTranslator extends Translator<GeometryCollection, EVSpatialGeometryCollection> {

    public GeometryCollectionTranslator() {
        super(GeometryCollection.class, EVSpatialGeometryCollection.class);
    }

    @Override
    protected EVSpatialGeometryCollection process(GeometryCollection in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        EVSpatialExpression[] translated = new EVSpatialExpression[in.getArity()];
        for (int i = 0; i < in.getArity(); i++) {
            translated[i] = translator.translateSpatialExpression(in.getInput(i), schema, bindings);
        }
        return new EVSpatialGeometryCollection(translated);
    }

}
