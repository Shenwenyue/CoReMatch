package CoReMatch.esp.api.epa.pattern.symbol;

import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.expression.BooleanExpression;

public class Symbol {

    private final char identifier;

    private final BooleanExpression condition;

    private final Bindings bindings;

    public Symbol(char identifier, BooleanExpression condition) {
        this(identifier, condition, new Bindings());
    }

    public Symbol(char identifier, BooleanExpression condition, Bindings bindings) {
        super();
        this.identifier = identifier;
        this.condition = condition;
        this.bindings = bindings;

    }

    public char getId() {
        return identifier;
    }

    public BooleanExpression getCondition() {
        return condition;
    }

    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public String toString() {
        return "[" + identifier + ":=" + condition + ", bindings=" + bindings + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bindings == null) ? 0 : bindings.hashCode());
        result = prime * result + ((condition == null) ? 0 : condition.hashCode());
        result = prime * result + identifier;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Symbol other = (Symbol) obj;
        if (bindings == null) {
            if (other.bindings != null)
                return false;
        } else if (!bindings.equals(other.bindings))
            return false;
        if (condition == null) {
            if (other.condition != null)
                return false;
        } else if (!condition.equals(other.condition))
            return false;
        if (identifier != other.identifier)
            return false;
        return true;
    }

}
