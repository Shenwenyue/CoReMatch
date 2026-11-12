package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.spatial.Crosses;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.predicate.spatial.EVSpatialCrosses;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class CrossesTranslator extends Translator<Crosses, EVPredicate> {

    public CrossesTranslator() {
        super(Crosses.class, EVPredicate.class);
    }

    @Override
    protected EVPredicate process(Crosses in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);
        ArithmeticExpression r = in.getInput(1);

        DataType lt = l.getDataType(schema, bindings);
        DataType rt = r.getDataType(schema, bindings);

        DataType gct = DataType.getGCT(lt, rt);

        if (gct == DataType.GEOMETRY) {
            EVSpatialExpression tl = translator.translateSpatialExpression(l, schema, bindings);
            EVSpatialExpression tr = translator.translateSpatialExpression(r, schema, bindings);
            return new EVSpatialCrosses(tl, tr);
        } else
            throw new IncompatibleTypeException("No implementation for Crosses with input-types: " + lt + " and " + rt);
    }
}

