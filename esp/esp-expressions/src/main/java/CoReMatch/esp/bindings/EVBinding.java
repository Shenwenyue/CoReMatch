package CoReMatch.esp.bindings;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.expressions.arithmetic.EVArithmeticExpression;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class EVBinding extends Binding {

    private final EVArithmeticExpression<?> expression;

    public EVBinding(Binding b, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        super(b.getName(), b.getValue(), b.getBindingTime());
        DataType dt = b.getValue().getDataType(schema, bindings);

        if (dt.isNumeric()) {
            this.expression = ExpressionTranslator.INSTANCE.translateNumericExpression(b.getValue(), schema, bindings);
        } else if (dt == DataType.STRING) {
            this.expression = ExpressionTranslator.INSTANCE.translateStringExpression(b.getValue(), schema, bindings);
        } else if (dt == DataType.SET) {
            this.expression = ExpressionTranslator.INSTANCE.translateSetExpression(b.getValue(), schema, bindings);
        } else if (dt == DataType.GEOMETRY) {
            this.expression = ExpressionTranslator.INSTANCE.translateSpatialExpression(b.getValue(), schema, bindings);
        } else
            throw new IncompatibleTypeException("Type " + dt + " not supported in VariableBindings");
    }

    public EVArithmeticExpression<?> getExpression() {
        return expression;
    }

}
