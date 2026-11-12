package CoReMatch.esp.expressions.predicate.string;

import CoReMatch.esp.expressions.arithmetic.EVStringExpression;
import CoReMatch.esp.expressions.predicate.EVPredicate;
import CoReMatch.esp.expressions.util.EVAbstractBinaryExpression;

/**
 * Compares two String inputs for Like Matching.
 */
public class EVStringLike extends EVAbstractBinaryExpression<String, String, Boolean> implements EVPredicate {

    public EVStringLike(EVStringExpression left, EVStringExpression right) {
        super(left, right, new OperatorImpl<String, String, Boolean>() {

            @Override
            public Boolean apply(String l, String regex) {
                regex = regex
                        .replace("\\", "\\\\") //escape special java chars 
                        .replace(".", "\\.").replace("^", "\\^")
                        .replace("$", "\\$").replace("|", "\\|")
                        .replace("{", "\\{").replace("}", "\\}")
                        .replaceAll("(?<!!)%", ".*") //replace % with .* (but not !%)
                        .replaceAll("(?<!!)_", ".") //replace _ with . (but not !_)
                        .replace("!%", "%")
                        .replace("!_", "_");
                return l.toLowerCase().matches(regex);
            }
        });
    }

}
