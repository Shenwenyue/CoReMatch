package CoReMatch.pattern.util;

import CoReMatch.db.util.TimeInterval;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;

import java.util.Iterator;

public class IntervalMergeCursor<T extends TimeInterval> extends AbstractCursor<T> {

    private final Cursor<T> src;

    private final int allowedDistance;

    private T nextResult;

    private T nextInput;

    public IntervalMergeCursor(Iterator<T> src) {
        this(src, 0);
    }

    private IntervalMergeCursor(Iterator<T> src, int allowedDistance) {
        this.src = Cursors.wrap(src);
        this.allowedDistance = allowedDistance;
    }

    @Override
    public void open() {
        if (isOpened)
            return;

        super.open();

        src.open();

        if (src.hasNext()) {
            nextInput = src.next();
            nextResult = computeNext();
        }
    }

    @Override
    public void close() {
        if (isClosed)
            return;

        super.close();
        src.close();
    }

    @Override
    protected boolean hasNextObject() {
        return nextResult != null;
    }

    @Override
    protected T nextObject() {
        var result = nextResult;
        nextResult = computeNext();
        return result;
    }

    private T computeNext() {
        if (nextInput == null)
            return null;

        T result = nextInput;
        while (src.hasNext()) {
            nextInput = src.next();

            if (nextInput.getT1() - result.getT2() - 1 <= allowedDistance)

                result = (T) result.adjust(result.getT1(), nextInput.getT2());

            else

                return result;
        }

        nextInput = null;
        return result;
    }
}
