package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.InteriorPoint;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialInteriorPoint;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class InteriorPointTranslator extends Translator<InteriorPoint, EVSpatialInteriorPoint> {

    public InteriorPointTranslator() {
        super(InteriorPoint.class, EVSpatialInteriorPoint.class);
    }

    @Override
    protected EVSpatialInteriorPoint process(InteriorPoint in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialInteriorPoint(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
