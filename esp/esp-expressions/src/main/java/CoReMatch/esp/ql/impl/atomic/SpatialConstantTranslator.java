package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.GeoConstant;
import CoReMatch.esp.expressions.arithmetic.constant.EVSpatialConstant;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;

@MetaInfServices
public class SpatialConstantTranslator extends Translator<GeoConstant, EVSpatialConstant> {

    public SpatialConstantTranslator() {
        super(GeoConstant.class, EVSpatialConstant.class);
    }

    @Override
    protected EVSpatialConstant process(GeoConstant in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException {
        return new EVSpatialConstant(in.getValue());
    }

}
