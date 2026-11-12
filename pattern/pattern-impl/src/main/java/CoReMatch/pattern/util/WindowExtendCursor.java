package CoReMatch.pattern.util;

import CoReMatch.db.util.TimeInterval;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;

public class WindowExtendCursor<T extends TimeInterval> extends AbstractCursor<T> {

    private final Cursor<T> in;

    private final long window;

    private final TransformedPattern.ExecutableConfiguration config;

    public WindowExtendCursor(Cursor<T> in, TransformedPattern.ExecutableConfiguration config, long window) {
        this.in = in;
        this.config = config;
        this.window = window;
    }

    @Override
    public void open() {
        if (isOpened)
            return;
        super.open();

        in.open();

    }

    @Override
    public void close() {
        if (isClosed)
            return;
        super.close();
        in.close();
    }

    @Override
    protected boolean hasNextObject() {
        return in.hasNext();
    }

    @Override
    protected T nextObject() {
        return config.getTScope(in.next(), window);
    }
}
