package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.Area;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialArea;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class AreaTranslator extends Translator<Area, EVSpatialArea> {

    public AreaTranslator() {
        super(Area.class, EVSpatialArea.class);
    }

    @Override
    protected EVSpatialArea process(Area in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialArea(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
