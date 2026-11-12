
/* XXL: The eXtensible and fleXible Library for data processing

Copyright (C) 2000-2011 Prof. Dr. Bernhard Seeger
                        Head of the Database Research Group
                        Department of Mathematics and Computer Science
                        University of Marburg
                        Germany

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library;  If not, see <http://www.gnu.org/licenses/>.

    http://code.google.com/p/xxl/

*/

package xxl.core.cursors;

import java.util.NoSuchElementException;

public abstract class AbstractCursor<E> implements Cursor<E> {

	protected boolean hasNext = false;

	protected boolean computedHasNext = false;

	protected boolean isValid = true;

	protected E next = null;

	protected boolean assignedNext = false;

	protected boolean isOpened = false;

	protected boolean isClosed = false;

	public void open() {
		if (!isOpened)
			isOpened = true;
	}

	public void close() {
		if (!isClosed) {
			hasNext = false;
			computedHasNext = false;
			isValid = false;
			isClosed = true;
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	public final boolean hasNext() throws IllegalStateException {
		if (isClosed)
			throw new IllegalStateException();
		if (!isOpened)
			open();
		if (!computedHasNext) {
			hasNext = hasNextObject();
			computedHasNext = true;
			isValid = false;
			assignedNext = false;
		}

		return hasNext;
	}

	protected abstract boolean hasNextObject();

	public final E next() throws IllegalStateException, NoSuchElementException {
		if (!computedHasNext)
			hasNext();
		if (!hasNext)
			throw new NoSuchElementException();
		if (!assignedNext) {
			next = nextObject();
			assignedNext = true;
		}
		hasNext = false;
		computedHasNext = false;
		isValid = true;
		return next;
	}

	protected abstract E nextObject();

	public final E peek() throws IllegalStateException, NoSuchElementException, UnsupportedOperationException {
		if (!supportsPeek())
			throw new UnsupportedOperationException();
		if (!computedHasNext)
			hasNext();
		if (!hasNext)
			throw new NoSuchElementException();
		if (!assignedNext) {
			next = nextObject();
			assignedNext = true;
		}
		isValid = true;
		return next;
	}

	public final boolean supportsPeek() {
		return true;
	}

	public void remove() throws IllegalStateException, UnsupportedOperationException {
		if (!supportsRemove())
			throw new UnsupportedOperationException();
		if (!isValid)
			throw new IllegalStateException();
		hasNext = false;
		computedHasNext = false;
		isValid = false;
		assignedNext = false;
	}

	public boolean supportsRemove() {
		return false;
	}

	public void update(E object) throws IllegalStateException, UnsupportedOperationException {
		if (!supportsUpdate())
			throw new UnsupportedOperationException();
		if (!isValid)
			throw new IllegalStateException();
		hasNext = false;
		computedHasNext = false;
		isValid = false;
		assignedNext = false;
	}

	public boolean supportsUpdate() {
		return false;
	}

	public void reset() throws UnsupportedOperationException {
		if (!supportsReset())
			throw new UnsupportedOperationException();
		hasNext = false;
		computedHasNext = false;
		isValid = false;
		assignedNext = false;
	}

	public boolean supportsReset() {
		return false;
	}
}
