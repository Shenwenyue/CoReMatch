package CoReMatch.esp.ql.impl.spatial.numeric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.numeric.NumGeometries;
import CoReMatch.esp.expressions.spatial.numeric.EVSpatialNumGeometries;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class NumGeometriesTranslator extends Translator<NumGeometries, EVSpatialNumGeometries> {

    public NumGeometriesTranslator() {
        super(NumGeometries.class, EVSpatialNumGeometries.class);
    }

    @Override
    protected EVSpatialNumGeometries process(NumGeometries in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialNumGeometries(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}

