package CoReMatch.esp.api.expression.arithmetic.atomic;

import CoReMatch.event.Attribute.DataType;

/**
 * A constant int value
 */
public class IntConstant extends Constant<Integer> {

    /**
     * @param value The value
     */
    public IntConstant(Integer value) {
        super(value, DataType.INTEGER);
    }

}
