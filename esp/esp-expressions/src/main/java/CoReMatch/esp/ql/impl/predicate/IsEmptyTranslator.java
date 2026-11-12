package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.spatial.IsEmpty;
import CoReMatch.esp.expressions.arithmetic.EVSpatialExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.predicate.spatial.EVSpatialIsEmpty;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class IsEmptyTranslator extends Translator<IsEmpty, EVPredicate> {

    public IsEmptyTranslator() {
        super(IsEmpty.class, EVPredicate.class);
    }

    @Override
    protected EVPredicate process(IsEmpty in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);

        DataType lt = l.getDataType(schema, bindings);

        if (lt == DataType.GEOMETRY) {
            EVSpatialExpression tl = translator.translateSpatialExpression(l, schema, bindings);
            return new EVSpatialIsEmpty(tl);
        } else
            throw new IncompatibleTypeException("No implementation for isEmpty with input-type: " + lt);
    }
}

