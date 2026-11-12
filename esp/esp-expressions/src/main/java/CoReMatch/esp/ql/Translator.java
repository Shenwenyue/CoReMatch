package CoReMatch.esp.ql;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.NoSuchVariableException;
import CoReMatch.esp.api.expression.Expression;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public abstract class Translator<TIN extends Expression, TOUT> {

    private final Class<TIN> inputType;
    private final Class<TOUT> outputType;

    public Translator(Class<TIN> inputType, Class<TOUT> outputType) {
        super();
        this.inputType = inputType;
        this.outputType = outputType;
    }

    public Class<TIN> getInputType() {
        return inputType;
    }

    public Class<TOUT> getOutputType() {
        return outputType;
    }

    public final TOUT translate(Expression in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, NoSuchVariableException, SchemaException {
        if (inputType.isAssignableFrom(in.getClass())) {
            return process(inputType.cast(in), translator, schema, bindings);
        } else
            throw new TranslatorException("Illegal input \"" + in.getClass() + " given to translator: " + getClass());
    }

    protected abstract TOUT process(TIN in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, NoSuchVariableException, SchemaException;

    @Override
    public String toString() {
        return getClass().getName() + ": " + inputType.getName() + " --> " + outputType.getName();
    }
}
