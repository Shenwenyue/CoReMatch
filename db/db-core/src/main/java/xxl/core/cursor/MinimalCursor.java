package xxl.core.cursor;

import java.util.NoSuchElementException;

import xxl.core.cursors.Cursor;

public interface MinimalCursor<T> extends Cursor<T> {

	@Override
	default T peek() throws IllegalStateException, NoSuchElementException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Peek not supported by this cursor (" + getClass().getName() + ").");
	}

	@Override
	default boolean supportsPeek() {
		return false;
	}

	@Override
	default void remove() throws IllegalStateException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Remove not supported by this cursor (" + getClass().getName() + ").");
	}

	@Override
	default boolean supportsRemove() {
		return false;
	}

	@Override
	default void update(T object) throws IllegalStateException, UnsupportedOperationException {
		throw new UnsupportedOperationException("Update not supported by this cursor (" + getClass().getName() + ").");
	}

	@Override
	default boolean supportsUpdate() {
		return false;
	}

	@Override
	default void reset() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Reset not supported by this cursor (" + getClass().getName() + ").");
	}

	@Override
	default boolean supportsReset() {
		return false;
	}
}
