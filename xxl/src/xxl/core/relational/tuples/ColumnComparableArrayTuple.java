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

package xxl.core.relational.tuples;

import java.util.Arrays;

public class ColumnComparableArrayTuple extends ArrayTuple
        implements ColumnComparableTuple {

  protected Object[] tuple;

  public ColumnComparableArrayTuple(Object... array) {
    for (Object c : array)
      if (c instanceof ColumnComparableArrayTuple) {
        this.tuple = ((ColumnComparableArrayTuple) c).toComparableArray();
        return;
      } else if (c instanceof Tuple) {
        this.tuple = (Comparable[]) c;
        return;
      }
    this.tuple = array;
  }

  public ColumnComparableArrayTuple(Tuple template) {
    Comparable destination[] = new Comparable[template.getColumnCount()];
    Object source[] = template.toArray();

    for (int i = 0; i < source.length; i++) {
      destination[i] = (Comparable) source[i];
    }
    this.tuple = destination;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    ColumnComparableArrayTuple clone =
        (ColumnComparableArrayTuple) super.clone();
    clone.tuple = tuple.clone();
    return clone;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public int compareTo(Object other) {

    if (!(other instanceof ColumnComparableArrayTuple)) {
      throw new ClassCastException("Cannot compare ColumnComparableArrayTuple with " + other.getClass().getName());
    }

    Comparable[] first =
        new ColumnComparableArrayTuple(tuple).toComparableArray();
    Comparable[] second =
        ((ColumnComparableArrayTuple) other).toComparableArray();

    for (int i = 0; i < Math.min(first.length, second.length); i++) {
      @SuppressWarnings("unchecked")
      int compare = first[i].compareTo(second[i]);
      if (compare != 0) return compare;
    }
    if (first.length < second.length)
      return -1;
    else if (first.length > second.length)
      return 1;
    else
      return 0;
  }

  @Override
  public int getColumnCount() {
    return tuple.length;
  }

  @Override
  public Object getObject(int columnIndex) {
    return tuple[columnIndex - 1];
  }

  @Override
  public Object[] toArray() {
    return tuple.clone();
  }

  @Override
  public Comparable[] toComparableArray() {

    try {
      Comparable[] result = new Comparable[tuple.length];
      for (int i = 0; i < tuple.length; i++)
        result[i] = (Comparable) tuple[i];
      return result;

    } catch (Exception e) {
      throw new RuntimeException(
          "The given tuple does not contain comparable objects at all.");
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(tuple);
  }
}
