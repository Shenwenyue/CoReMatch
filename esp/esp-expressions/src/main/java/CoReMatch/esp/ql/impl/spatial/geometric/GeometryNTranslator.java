package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.GeometryN;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialGeometryN;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class GeometryNTranslator extends Translator<GeometryN, EVSpatialGeometryN> {

    public GeometryNTranslator() {
        super(GeometryN.class, EVSpatialGeometryN.class);
    }

    @Override
    protected EVSpatialGeometryN process(GeometryN in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialGeometryN(translator.translateSpatialExpression(in.getInput(0), schema, bindings), in.getN());
    }

}
