package CoReMatch.esp.ql.impl.spatial.factory;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.factory.SpatialLineProjection;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.spatial.factory.EVSpatialLineProjection;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.ArrayList;
import java.util.List;

@MetaInfServices
public class SpatialLineProjectionTranslator extends Translator<SpatialLineProjection, EVSpatialLineProjection> {

    public SpatialLineProjectionTranslator() {
        super(SpatialLineProjection.class, EVSpatialLineProjection.class);
    }

    @Override
    protected EVSpatialLineProjection process(SpatialLineProjection in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        List<EVSpatialExpression> input = new ArrayList<>();
        for (int i = 0; i < in.getArity(); i++) {
            input.add(translator.translateSpatialExpression(in.getInput(i), schema, bindings));
        }
        return new EVSpatialLineProjection(input);
    }

}
