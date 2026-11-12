package CoReMatch.esp.api.expression.arithmetic.atomic;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

/**
 * A variable referencing one of:
 * <ul>
 * <li>An event attribute</li>
 * <li>A variable bound while processing</li>
 * </ul>
 */
public class Variable implements ArithmeticExpression {

    /**
     * The variable name
     */
    private final String name;

    /**
     * @param name The variable name
     */
    public Variable(String name) {
        this.name = name;
    }

    /**
     * @return the name of this variable
     */
    public String getName() {
        return name;
    }

    @Override
    public int getArity() {
        return 0;
    }

    @Override
    public int getRequiredNumberOfPreviousEvents() {
        return 0;
    }

    @Override
    public ArithmeticExpression getInput(int index) {
        throw new IndexOutOfBoundsException("Variables values have no inputs");
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        try {
            return schema.byName(name).getType();
        } catch (SchemaException nsv) {
            Binding b = bindings.byName(name);
            return b.getValue().getDataType(schema, bindings.without(b));
        }
    }

    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Variable variable = (Variable) o;

        return name != null ? name.equals(variable.name) : variable.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
