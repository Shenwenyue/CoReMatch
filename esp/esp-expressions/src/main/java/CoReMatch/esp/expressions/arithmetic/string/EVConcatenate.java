package CoReMatch.esp.expressions.arithmetic.string;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;
import CoReMatch.event.Attribute.DataType;

public class EVConcatenate extends EVAbstractBinaryExpression<String, String, String> implements EVStringExpression {

    public EVConcatenate(EVStringExpression left, EVStringExpression right) {
        super(left, right, (String x, String y) -> ((String) x) + " " + ((String) y));
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }
}
