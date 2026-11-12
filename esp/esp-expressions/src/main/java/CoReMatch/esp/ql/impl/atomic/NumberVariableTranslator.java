package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.Variable;
import CoReMatch.esp.expressions.arithmetic.field.*;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class NumberVariableTranslator extends Translator<Variable, EVNumericVariable> {

    public NumberVariableTranslator() {
        super(Variable.class, EVNumericVariable.class);
    }

    @Override
    protected EVNumericVariable process(Variable in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, SchemaException {
        int index = -1;
        DataType type = null;
        boolean isBinding = false;

        try {
            index = schema.getAttributeIndex(in.getName());
            type = schema.getAttribute(index).getType();
        } catch (SchemaException se) {
            Binding b = bindings.byName(in.getName());
            index = bindings.indexOf(b.getName());
            try {
                type = b.getValue().getDataType(schema, bindings.without(b));
            } catch (IncompatibleTypeException e) {
                throw new TranslatorException("Binding has incompatible type", e);
            }
            isBinding = true;
        }
        switch (type) {
            case BYTE:
                return new EVByteVariable(index, isBinding);
            case SHORT:
                return new EVShortVariable(index, isBinding);
            case INTEGER:
                return new EVIntVariable(index, isBinding);
            case LONG:
                return new EVLongVariable(index, isBinding);
            case FLOAT:
                return new EVFloatVariable(index, isBinding);
            case DOUBLE:
                return new EVDoubleVariable(index, isBinding);
            default:
                throw new TranslatorException("Not a numeric attribute: " + in.getName());
        }
    }


}
