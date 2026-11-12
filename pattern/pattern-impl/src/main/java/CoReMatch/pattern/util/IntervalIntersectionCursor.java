package CoReMatch.pattern.util;

import CoReMatch.db.util.TimeInterval;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;

import java.util.Iterator;

public class IntervalIntersectionCursor extends AbstractCursor<TimeInterval> {

    private final Cursor<TimeInterval> in1, in2;

    private TimeInterval current1, current2, next;

    public IntervalIntersectionCursor(Iterator<TimeInterval> in1, Iterator<TimeInterval> in2) {
        this.in1 = Cursors.wrap(in1);
        this.in2 = Cursors.wrap(in2);
    }

    @Override
    public void open() {
        if (isOpened)
            return;

        super.open();
        in1.open();
        in2.open();

        if (in1.hasNext())
            current1 = in1.next();
        if (in2.hasNext())
            current2 = in2.next();

        next = computeNext();

    }

    @Override
    public void close() {
        super.close();
    }

    private TimeInterval computeNext() {
        TimeInterval result = null;

        while (current1 != null && current2 != null && result == null) {

            if (current1.intersects(current2)) {

                result = current1.intersect(current2);
            }

            if (current1.getT2() >= current2.getT2()) {

                current2 = in2.hasNext() ? in2.next() : null;
            } else {

                current1 = in1.hasNext() ? in1.next() : null;
            }
        }

        if (result != null) {

        } else {

        }
        return result;
    }

    @Override
    protected boolean hasNextObject() {
        return next != null;
    }

    @Override
    protected TimeInterval nextObject() {
        var res = next;
        next = computeNext();
        return res;
    }

}
