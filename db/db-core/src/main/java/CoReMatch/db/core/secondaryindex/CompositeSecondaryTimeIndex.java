package CoReMatch.db.core.secondaryindex;

import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

public interface CompositeSecondaryTimeIndex<K1 extends Comparable<K1>, K2 extends Comparable<K2>>
        extends SecondaryTimeIndex<CompositeKey<K1, K2>> {

    String getAttribute1Name();

    String getAttribute2Name();

    Cursor<Pair<CompositeKey<K1, K2>, EventID>> rangeQueryByID1(K1 minKey1, K1 maxKey1);

    Cursor<Pair<CompositeKey<K1, K2>, EventID>> rangeQueryByCompositeKey(
            K1 minKey, K1 maxKey, K2 minKey2, K2 maxKey2);
}
