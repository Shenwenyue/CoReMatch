package CoReMatch.esp.ql.impl.arithmetic.string;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.arithmetic.compund.string.StringConcatenate;
import CoReMatch.esp.expressions.arithmetic.string.EVConcatenate;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class ConcatenateTranslator extends Translator<StringConcatenate, EVConcatenate> {

    public ConcatenateTranslator() {
        super(StringConcatenate.class, EVConcatenate.class);
    }

    @Override
    protected EVConcatenate process(StringConcatenate in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, SchemaException, IncompatibleTypeException {
        ArithmeticExpression left = in.getInput(0);
        ArithmeticExpression right = in.getInput(1);
        if (left.getDataType(schema, bindings) == DataType.STRING && right.getDataType(schema, bindings) == DataType.STRING)
            return new EVConcatenate(translator.translateStringExpression(left, schema, bindings), translator.translateStringExpression(right, schema, bindings));
        else
            throw new TranslatorException("Both inputs must be string types ");

    }

}
