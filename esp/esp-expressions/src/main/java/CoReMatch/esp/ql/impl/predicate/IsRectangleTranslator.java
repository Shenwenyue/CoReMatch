package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.spatial.IsRectangle;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.predicate.spatial.EVSpatialIsRectangle;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class IsRectangleTranslator extends Translator<IsRectangle, EVPredicate> {

    public IsRectangleTranslator() {
        super(IsRectangle.class, EVPredicate.class);
    }

    @Override
    protected EVPredicate process(IsRectangle in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);

        DataType lt = l.getDataType(schema, bindings);

        if (lt == DataType.GEOMETRY) {
            EVSpatialExpression tl = translator.translateSpatialExpression(l, schema, bindings);
            return new EVSpatialIsRectangle(tl);
        } else
            throw new IncompatibleTypeException("No implementation for isRectangle with input-type: " + lt);
    }
}
