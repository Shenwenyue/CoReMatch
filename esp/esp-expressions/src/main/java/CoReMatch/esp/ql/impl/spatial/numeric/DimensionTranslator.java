package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.Dimension;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialDimension;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class DimensionTranslator extends Translator<Dimension, EVSpatialDimension> {

    public DimensionTranslator() {
        super(Dimension.class, EVSpatialDimension.class);
    }

    @Override
    protected EVSpatialDimension process(Dimension in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialDimension(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}

