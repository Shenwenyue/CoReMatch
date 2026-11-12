package CoReMatch.pattern.cost.transform;

import java.util.List;

public class SubPattern {

    private final int index;

    private final boolean exactPossible;

    private final int length;

    private final List<SubPatternCondition<?>> conditions;

    public SubPattern(int index, int length, boolean exactPossible, List<SubPatternCondition<?>> conditions) {
        this.index = index;
        this.length = length;
        this.conditions = conditions;
        this.exactPossible = exactPossible;
    }

    public int getIndex() {
        return this.index;
    }

    public int getLength() {
        return this.length;
    }

    public List<SubPatternCondition<?>> getConditions() {
        return this.conditions;
    }

    public boolean isExactPossible() {
        return this.exactPossible;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.conditions == null) ? 0 : this.conditions.hashCode());
        result = prime * result + (this.exactPossible ? 1231 : 1237);
        result = prime * result + this.index;
        result = prime * result + this.length;
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
        SubPattern other = (SubPattern) obj;
        if (this.conditions == null) {
            if (other.conditions != null)
                return false;
        } else if (!this.conditions.equals(other.conditions))
            return false;
        if (this.exactPossible != other.exactPossible)
            return false;
        if (this.index != other.index)
            return false;
        if (this.length != other.length)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("SubPattern [index: %s, length: %s, exactPossible: %s, conditions: %s]", this.index,
                this.length, this.exactPossible, this.conditions);
    }

}
