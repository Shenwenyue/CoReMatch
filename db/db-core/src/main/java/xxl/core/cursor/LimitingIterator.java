package xxl.core.cursor;

import java.util.Iterator;

public class LimitingIterator<T> implements Iterator<T> {

	private final long limit;

	private final Iterator<T> input;

	private long count;

	public LimitingIterator(long limit, Iterator<T> input) {
		this.limit = limit;
		this.input = input;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext() && count < limit;
	}

	@Override
	public T next() {
		T res = input.next();
		count++;
		return res;
	}

}
