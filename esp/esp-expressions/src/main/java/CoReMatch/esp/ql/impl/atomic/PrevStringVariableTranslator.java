package CoReMatch.esp.ql.impl.atomic;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.arithmetic.atomic.PrevVariable;
import CoReMatch.esp.expressions.arithmetic.field.EVStringPrevVariable;
import CoReMatch.esp.expressions.arithmetic.field.EVStringVariable;
import CoReMatch.esp.expressions.arithmetic.field.EVVariable;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class PrevStringVariableTranslator extends Translator<PrevVariable, EVStringPrevVariable> {

    public PrevStringVariableTranslator() {
        super(PrevVariable.class, EVStringPrevVariable.class);
    }

    @Override
    protected EVStringPrevVariable process(PrevVariable in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, SchemaException {
        int index = schema.getAttributeIndex(in.getName());
        Attribute.DataType type = schema.getAttribute(index).getType();
        boolean isBinding = false;
        int prev = in.getPrev();
        EVVariable<String> variable = null;

        if (type == Attribute.DataType.STRING)
            variable = new EVStringVariable(index, isBinding);
        else
            throw new TranslatorException("Not a string-variable: " + in.getName());

        return new EVStringPrevVariable(index, type, prev, variable);
    }
}
