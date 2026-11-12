package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.ConvexHull;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialConvexHull;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ConvexHullTranslator extends Translator<ConvexHull, EVSpatialConvexHull> {

    public ConvexHullTranslator() {
        super(ConvexHull.class, EVSpatialConvexHull.class);
    }

    @Override
    protected EVSpatialConvexHull process(ConvexHull in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialConvexHull(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
