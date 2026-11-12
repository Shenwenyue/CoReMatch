package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.NumPoints;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialNumPoints;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class NumPointsTranslator extends Translator<NumPoints, EVSpatialNumPoints> {

    public NumPointsTranslator() {
        super(NumPoints.class, EVSpatialNumPoints.class);
    }

    @Override
    protected EVSpatialNumPoints process(NumPoints in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialNumPoints(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
