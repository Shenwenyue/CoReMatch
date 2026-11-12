package CoReMatch.esp.api.expression.arithmetic.atomic;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.Objects;

/**
 * A prev variable referencing an event attribute
 */
public class PrevVariable implements ArithmeticExpression {

    /**
     * The variable name
     */
    private final String name;

    /**
     * The physical offset in the stream
     */
    private final int prev;

    /**
     * @param name The variable name
     */
    public PrevVariable(String name, int prev) {
        this.name = name;
        this.prev = prev;
    }

    /**
     * @return the name of this variable
     */
    public String getName() {
        return name;
    }

    /**
     * @return the physical offset in the stream
     */
    public int getPrev() {
        return prev;
    }

    @Override
    public int getRequiredNumberOfPreviousEvents() {
        return prev;
    }

    @Override
    public int getArity() {
        return 0;
    }

    @Override
    public ArithmeticExpression getInput(int index) {
        throw new IndexOutOfBoundsException("Variables values have no inputs");
    }

    @Override
    public Attribute.DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException, SchemaException {
        try {
            return schema.byName(name).getType();
        } catch (SchemaException nsv) {
            Binding b = bindings.byName(name);
            return b.getValue().getDataType(schema, bindings.without(b));
        }
    }

    public String toString() {
        return "(" + name + "," + prev + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrevVariable that = (PrevVariable) o;
        return prev == that.prev &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, prev);
    }
}
