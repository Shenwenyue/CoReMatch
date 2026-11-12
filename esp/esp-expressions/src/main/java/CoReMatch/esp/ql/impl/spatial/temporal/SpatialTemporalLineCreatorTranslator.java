package CoReMatch.esp.ql.impl.spatial.temporal;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.NoSuchVariableException;
import CoReMatch.esp.api.expression.spatial.temporal.SpatialTemporalLineCreator;
import CoReMatch.esp.expressions.spatial.temporal.EVSpatialTemporalLineCreator;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class SpatialTemporalLineCreatorTranslator extends Translator<SpatialTemporalLineCreator, EVSpatialTemporalLineCreator> {

    public SpatialTemporalLineCreatorTranslator() {
        super(SpatialTemporalLineCreator.class, EVSpatialTemporalLineCreator.class);
    }

    @Override
    protected EVSpatialTemporalLineCreator process(SpatialTemporalLineCreator in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, NoSuchVariableException, SchemaException {
        return new EVSpatialTemporalLineCreator(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
