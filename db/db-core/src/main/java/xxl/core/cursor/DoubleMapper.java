package xxl.core.cursor;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Mapper implementation for double cursors.
 *
 */
public class DoubleMapper<I,E> implements DoubleCursor<E> {

	private final Function<? super I,? extends E> mapFunction;
	private final Function<? super E,? extends I> mapBack;

	private final DoubleCursor<I> input;

	public DoubleMapper(Function<? super I, ? extends E> function, DoubleCursor<I> input) {
		this(function,null,input);
	}

	public DoubleMapper(Function<? super I, ? extends E> mapFunction, Function<? super E, ? extends I> mapBack,
			DoubleCursor<I> input) {
		this.mapFunction = mapFunction;
		this.mapBack = mapBack;
		this.input = input;
	}

	@Override
	public void open() {
		input.open();
	}

	@Override
	public void close() {
		input.close();
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public E next() {
		return mapFunction.apply(input.next());
	}

	@Override
	public E peek() throws IllegalStateException, NoSuchElementException, UnsupportedOperationException {
		return mapFunction.apply(input.peek());
	}

	@Override
	public void remove() throws IllegalStateException, UnsupportedOperationException {
		input.remove();
	}

	@Override
	public void update(E object) throws IllegalStateException, UnsupportedOperationException {
		if ( !supportsUpdate() )
			throw new UnsupportedOperationException("Update not supported.");

		input.update( mapBack.apply(object) );

	}

	@Override
	public void reset() throws UnsupportedOperationException {
		input.reset();
	}

	@Override
	public E peekBack() {
		return mapFunction.apply(input.peekBack());
	}

	@Override
	public E previous() {
		return mapFunction.apply(input.previous());
	}

	@Override
	public boolean hasPrevious() {
		return input.hasPrevious();
	}

	@Override
	public boolean supportsPeek() {
		return input.supportsPeek();
	}

	@Override
	public boolean supportsPeekBack() {
		return input.supportsPeekBack();
	}

	@Override
	public boolean supportsRemove() {
		return input.supportsRemove();
	}

	@Override
	public boolean supportsUpdate() {
		return input.supportsUpdate() && mapBack != null;
	}

	@Override
	public boolean supportsReset() {
		return input.supportsReset();
	}

}
