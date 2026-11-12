package xxl.core.cursor;

import java.util.function.Function;

import xxl.core.cursors.AbstractCursor;
import xxl.core.cursors.Cursor;

/**
 * Mapper implementation for double cursors.
 *
 */
public class SimpleMapper<I,E> extends AbstractCursor<E> {

	private final Function<? super I,? extends E> mapFunction;

	private final Cursor<? extends I> input;

	public SimpleMapper(Function<? super I, ? extends E> function, Cursor<? extends I> input) {
		this.mapFunction = function;
		this.input = input;
	}

	@Override
	public void open() {
		if ( isOpened )
			return;

		super.open();
		input.open();
	}

	@Override
	public void close() {
		if ( isClosed )
			return;
		super.close();
		input.close();
	}

	@Override
	protected boolean hasNextObject() {
		return input.hasNext();
	}

	@Override
	protected E nextObject() {
		return mapFunction.apply(input.next());
	}

}
