package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.Unequal;
import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.predicate.numeric.EVNumericUnequal;
import CoReMatch.esp.expressions.predicate.string.EVStringUnequal;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class UnEqualTranslator extends Translator<Unequal, EVPredicate> {

    public UnEqualTranslator() {
        super(Unequal.class, EVPredicate.class);
    }

    @Override
    protected EVPredicate process(Unequal in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);
        ArithmeticExpression r = in.getInput(1);
        DataType lt = l.getDataType(schema, bindings);
        DataType rt = r.getDataType(schema, bindings);

        DataType gct = DataType.getGCT(lt, rt);

        if (gct.isNumeric()) {
            EVNumericExpression tl = translator.translateNumericExpression(l, schema, bindings);
            EVNumericExpression tr = translator.translateNumericExpression(r, schema, bindings);
            return new EVNumericUnequal(gct, tl, tr);
        } else if (gct == DataType.STRING) {
            EVStringExpression tl = translator.translateStringExpression(l, schema, bindings);
            EVStringExpression tr = translator.translateStringExpression(r, schema, bindings);
            return new EVStringUnequal(tl, tr);
        } else
            throw new IncompatibleTypeException("No implementation for Unequal with input-types: " + lt + ", " + rt);
    }
}
