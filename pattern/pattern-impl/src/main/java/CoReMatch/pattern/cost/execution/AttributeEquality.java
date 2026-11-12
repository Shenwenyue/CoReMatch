package CoReMatch.pattern.cost.execution;

/**
 * Residual constraint that two pattern symbols carry the same attribute value,
 * checked after a candidate group is materialized.
 */
public final class AttributeEquality {

    private final char leftSymbol;
    private final String leftAttribute;
    private final char rightSymbol;
    private final String rightAttribute;

    public AttributeEquality(char leftSymbol, String leftAttribute, char rightSymbol, String rightAttribute) {
        this.leftSymbol = Character.toUpperCase(leftSymbol);
        this.leftAttribute = leftAttribute.toUpperCase();
        this.rightSymbol = Character.toUpperCase(rightSymbol);
        this.rightAttribute = rightAttribute.toUpperCase();
    }

    public char getLeftSymbol() {
        return leftSymbol;
    }

    public String getLeftAttribute() {
        return leftAttribute;
    }

    public char getRightSymbol() {
        return rightSymbol;
    }

    public String getRightAttribute() {
        return rightAttribute;
    }

    /** Output-event field produced by the symbol binding {@code A_BEAT}. */
    public String leftOutputName() {
        return leftSymbol + "_" + outputSuffix(leftAttribute);
    }

    public String rightOutputName() {
        return rightSymbol + "_" + outputSuffix(rightAttribute);
    }

    /** Symbol bindings use LAT/LON; the event schema uses LATITUDE/LONGITUDE. */
    private static String outputSuffix(String attribute) {
        if ("LATITUDE".equals(attribute)) {
            return "LAT";
        }
        if ("LONGITUDE".equals(attribute)) {
            return "LON";
        }
        return attribute;
    }
}
