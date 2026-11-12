package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.Union;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialUnion;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class UnionTranslator extends Translator<Union, EVSpatialUnion> {

    public UnionTranslator() {
        super(Union.class, EVSpatialUnion.class);
    }

    @Override
    protected EVSpatialUnion process(Union in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialUnion(
                translator.translateSpatialExpression(in.getInput(0), schema, bindings),
                translator.translateSpatialExpression(in.getInput(1), schema, bindings));
    }

}
