package CoReMatch.db.core.secondaryindex;

import xxl.core.io.converters.MeasuredConverter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class EventID implements Comparable<EventID> {

    private final long blockId;

    private final int offset;

    private final long timestamp;

    private final long sequenceId;

    public EventID(Long blockId, int offset, long timestamp, long sequenceId) {
        this.blockId = blockId;
        this.offset = offset;
        this.timestamp = timestamp;
        this.sequenceId = sequenceId;

    }

    public static MeasuredConverter<EventID> getConverter() {
        return new MeasuredConverter<EventID>() {

            private static final long serialVersionUID = 1L;

            @Override
            public EventID read(DataInput dataInput, EventID object) throws IOException {
                return new EventID(dataInput.readLong(), dataInput.readInt(), dataInput.readLong(), dataInput.readLong());
            }

            @Override
            public void write(DataOutput dataOutput, EventID object) throws IOException {
                dataOutput.writeLong(object.blockId);
                dataOutput.writeInt(object.offset);
                dataOutput.writeLong(object.timestamp);
                dataOutput.writeLong(object.sequenceId);

            }

            @Override
            public int getMaxObjectSize() {
                return 28;
            }

        };
    }

    public long getBlockId() {
        return this.blockId;
    }

    public int getOffset() {
        return this.offset;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getSequenceId() {
        return this.sequenceId;
    }

    @Override
    public String toString() {
        return "blockId: " + blockId + ", offset: " + offset + ", timestamp:" + timestamp + ", sequenceId:" + sequenceId;
    }

    @Override
    public int compareTo(EventID o) {
        return Long.compare(sequenceId, o.sequenceId);

    }
}
