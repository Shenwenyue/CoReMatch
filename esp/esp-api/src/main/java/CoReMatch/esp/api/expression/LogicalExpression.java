package CoReMatch.esp.api.expression;

/**
 * Interface for logical operator (e.g. AND, OR, ...)
 */
public interface LogicalExpression extends BooleanExpression {

    BooleanExpression getInput(int index);

}
