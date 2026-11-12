package CoReMatch.db.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class TID implements Comparable<TID> {

    private final long blockId;

    private final int offset;

    public TID(final long blockId, final int offset) {
        this.blockId = blockId;
        this.offset = offset;
    }

    public long getBlockId() {
        return this.blockId;
    }

    public int getOffset() {
        return this.offset;
    }

    @Override
    public int compareTo(final TID o) {
        if (this.blockId == o.blockId)
            return Integer.compare(this.offset, o.offset);

        else
            return Long.compare(this.blockId, o.blockId);

    }

    @Override
    public String toString() {
        return String.format("%d:%d", blockId, offset);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.blockId ^ (this.blockId >>> 32));
        result = prime * result + this.offset;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TID other = (TID) obj;
        if (this.blockId != other.blockId)
            return false;
        if (this.offset != other.offset)
            return false;
        return true;
    }

}
