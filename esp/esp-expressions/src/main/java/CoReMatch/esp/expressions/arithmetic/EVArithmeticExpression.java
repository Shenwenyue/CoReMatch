package CoReMatch.esp.expressions.arithmetic;

import CoReMatch.esp.expressions.EvaluableExpression;
import CoReMatch.event.Attribute.DataType;

public interface EVArithmeticExpression<T> extends EvaluableExpression<T> {

    DataType getType();

}
