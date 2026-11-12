package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.spatial.IsWithinDistance;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.predicate.spatial.EVSpatialIsWithinDistance;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class IsWithinDistanceTranslator extends Translator<IsWithinDistance, EVPredicate> {

    public IsWithinDistanceTranslator() {
        super(IsWithinDistance.class, EVPredicate.class);
    }

    @Override
    protected EVPredicate process(IsWithinDistance in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);
        ArithmeticExpression r = in.getInput(1);
        double distance = in.getDistance();

        DataType lt = l.getDataType(schema, bindings);
        DataType rt = r.getDataType(schema, bindings);

        DataType gct = DataType.getGCT(lt, rt);

        if (gct == DataType.GEOMETRY) {
            EVSpatialExpression tl = translator.translateSpatialExpression(l, schema, bindings);
            EVSpatialExpression tr = translator.translateSpatialExpression(r, schema, bindings);
            return new EVSpatialIsWithinDistance(tl, tr, distance);
        } else
            throw new IncompatibleTypeException("No implementation for IsWithinDistance with input-types: " + lt + " and " + rt);
    }
}

