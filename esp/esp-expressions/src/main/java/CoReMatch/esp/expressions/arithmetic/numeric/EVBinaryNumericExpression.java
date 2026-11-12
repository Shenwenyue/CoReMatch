package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVBinaryNumericExpression extends EVAbstractBinaryExpression<Number, Number, Number>
        implements EVNumericExpression {

    private final DataType type;

    public EVBinaryNumericExpression(EvaluableExpression<Number> left, EvaluableExpression<Number> right, DataType type, OperatorImpl<Number, Number, Number> impl) {
        super(left, right, impl);
        this.type = type;
    }

    @Override
    public DataType getType() {
        return type;
    }

}
