package CoReMatch.esp.api.expression.arithmetic.atomic;

import CoReMatch.event.Attribute.DataType;

/**
 * A constant string value
 */
public class StringConstant extends Constant<String> {

    /**
     * @param value The value
     */
    public StringConstant(String value) {
        super(value, DataType.STRING);
    }

    @Override
    public String toString() {
        return value == null ? "null" : "'" + value + "'";
    }
}
