package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.BoundaryDimension;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialBoundaryDimension;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class BoundaryDimensionTranslator extends Translator<BoundaryDimension, EVSpatialBoundaryDimension> {

    public BoundaryDimensionTranslator() {
        super(BoundaryDimension.class, EVSpatialBoundaryDimension.class);
    }

    @Override
    protected EVSpatialBoundaryDimension process(BoundaryDimension in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialBoundaryDimension(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}

