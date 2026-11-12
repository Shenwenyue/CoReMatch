package CoReMatch.db.core.secondaryindex;

import xxl.core.io.converters.MeasuredConverter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CompositeKey<K1 extends Comparable<K1>, K2 extends Comparable<K2>>
        implements Comparable<CompositeKey<K1, K2>> {

    private final K1 key1;
    private final K2 key2;

    public CompositeKey(K1 key1, K2 key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    public K1 getKey1() {
        return key1;
    }

    public K2 getKey2() {
        return key2;
    }

    @Override
    public int compareTo(CompositeKey<K1, K2> other) {

        int cmp = this.key1.compareTo(other.key1);
        if (cmp != 0) {
            return cmp;
        }

        return this.key2.compareTo(other.key2);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CompositeKey<?, ?> other = (CompositeKey<?, ?>) obj;
        return key1.equals(other.key1) && key2.equals(other.key2);
    }

    @Override
    public int hashCode() {
        return 31 * key1.hashCode() + key2.hashCode();
    }

    @Override
    public String toString() {
        return "(" + key1 + ", " + key2 + ")";
    }

    public static <K1 extends Comparable<K1>, K2 extends Comparable<K2>>
            MeasuredConverter<CompositeKey<K1, K2>> getConverter(final MeasuredConverter<K1> key1Converter, final MeasuredConverter<K2> key2Converter) {

        return new MeasuredConverter<CompositeKey<K1, K2>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getMaxObjectSize() {
                return key1Converter.getMaxObjectSize() + key2Converter.getMaxObjectSize();
            }

            @Override
            public CompositeKey<K1, K2> read(DataInput dataInput, CompositeKey<K1, K2> object) throws IOException {
                K1 k1 = key1Converter.read(dataInput);
                K2 k2 = key2Converter.read(dataInput);
                return new CompositeKey<>(k1, k2);
            }

            @Override
            public void write(DataOutput dataOutput, CompositeKey<K1, K2> object) throws IOException {
                key1Converter.write(dataOutput, object.getKey1());
                key2Converter.write(dataOutput, object.getKey2());
            }
        };
    }
}
