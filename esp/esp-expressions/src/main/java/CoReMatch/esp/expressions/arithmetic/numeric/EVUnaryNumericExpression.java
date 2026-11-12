package CoReMatch.esp.expressions.arithmetic.numeric;

import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.esp.expressions.arithmetic.EVNumericExpression;
import CoReMatch.esp.expressions.util.EVAbstractUnaryExpression;
import CoReMatch.event.Attribute.DataType;

public abstract class EVUnaryNumericExpression extends EVAbstractUnaryExpression<Number, Number>
        implements EVNumericExpression {

    private final DataType type;

    public EVUnaryNumericExpression(EvaluableExpression<Number> input, DataType type, OperatorImpl<Number, Number> impl) {
        super(input, impl);
        this.type = type;
    }

    @Override
    public DataType getType() {
        return type;
    }
}
