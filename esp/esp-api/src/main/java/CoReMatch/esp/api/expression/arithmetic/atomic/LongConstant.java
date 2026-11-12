package CoReMatch.esp.api.expression.arithmetic.atomic;

import CoReMatch.event.Attribute.DataType;

/**
 * A constant long value
 */
public class LongConstant extends Constant<Long> {

    /**
     * @param value The value
     */
    public LongConstant(Long value) {
        super(value, DataType.LONG);
    }

}
