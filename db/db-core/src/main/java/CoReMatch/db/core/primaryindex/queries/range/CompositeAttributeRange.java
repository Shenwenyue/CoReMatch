package CoReMatch.db.core.primaryindex.queries.range;

import CoReMatch.db.core.secondaryindex.CompositeKey;
import CoReMatch.event.Attribute;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

public class CompositeAttributeRange<K1 extends Comparable<K1>, K2 extends Comparable<K2>> {

    private final AttributeRange<K1> range1;
    private final AttributeRange<K2> range2;

    private final CompositeKey<K1, K2> lowerComposite;
    private final CompositeKey<K1, K2> upperComposite;

    public CompositeAttributeRange(AttributeRange<K1> range1, AttributeRange<K2> range2) {
        this.range1 = range1;
        this.range2 = range2;

        this.lowerComposite = new CompositeKey<>(range1.getLower(), range2.getLower());
        this.upperComposite = new CompositeKey<>(range1.getUpper(), range2.getUpper());
    }

    public String getName() {
        return range1.getName() + "_" + range2.getName();
    }

    public AttributeRange<K1> getRange1() {
        return range1;
    }

    public AttributeRange<K2> getRange2() {
        return range2;
    }

    public String getAttribute1Name() {
        return range1.getName();
    }

    public String getAttribute2Name() {
        return range2.getName();
    }

    public CompositeKey<K1, K2> getLower() {
        return lowerComposite;
    }

    public CompositeKey<K1, K2> getUpper() {
        return upperComposite;
    }

    public boolean contains(K1 key1, K2 key2) {
        return range1.contains(key1) && range2.contains(key2);
    }

    public boolean contains(CompositeKey<K1, K2> key) {
        return contains(key.getKey1(), key.getKey2());
    }

    public String getDataTypeString() {
        return range1.getDataType() + "_" + range2.getDataType();
    }

    @Override
    public String toString() {
        return String.format("%s:%s [%s, %s], %s:%s [%s, %s]",
                range1.getName(), range1.getDataType(), range1.getLower(), range1.getUpper(),
                range2.getName(), range2.getDataType(), range2.getLower(), range2.getUpper());
    }

    public String toCompactString() {
        return String.format("%s:%s [%s, %s], %s:%s [%s, %s]",
                range1.getName(), range1.getDataType(), range1.getLower(), range1.getUpper(),
                range2.getName(), range2.getDataType(), range2.getLower(), range2.getUpper());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((range1 == null) ? 0 : range1.hashCode());
        result = prime * result + ((range2 == null) ? 0 : range2.hashCode());
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
        CompositeAttributeRange<?, ?> other = (CompositeAttributeRange<?, ?>) obj;
        if (range1 == null) {
            if (other.range1 != null)
                return false;
        } else if (!range1.equals(other.range1))
            return false;
        if (range2 == null) {
            if (other.range2 != null)
                return false;
        } else if (!range2.equals(other.range2))
            return false;
        return true;
    }
}
