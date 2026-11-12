package CoReMatch.esp.api.expression;

/**
 * Interface for any predicate.
 */
public interface Predicate extends BooleanExpression {

    ArithmeticExpression getInput(int index);

}
