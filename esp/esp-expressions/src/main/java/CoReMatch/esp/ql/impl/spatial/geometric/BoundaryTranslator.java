package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.Boundary;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialBoundary;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class BoundaryTranslator extends Translator<Boundary, EVSpatialBoundary> {

    public BoundaryTranslator() {
        super(Boundary.class, EVSpatialBoundary.class);
    }

    @Override
    protected EVSpatialBoundary process(Boundary in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialBoundary(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}

