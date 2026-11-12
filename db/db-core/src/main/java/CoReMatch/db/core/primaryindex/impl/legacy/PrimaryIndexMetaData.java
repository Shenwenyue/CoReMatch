package CoReMatch.db.core.primaryindex.impl.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.TimeRepresentation;
import xxl.core.indexStructures.BPlusTree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class PrimaryIndexMetaData {

    private static final Logger log = LoggerFactory.getLogger(PrimaryIndexMetaData.class);

    private static final byte VERSION_1 = 1;

    public static final byte CURRENT_VERSION = VERSION_1;

    private final EventSchema schema;

    private final TimeRepresentation timeRepresentation;

    private final ImmutableParams immutableParams;

    private MutableParams mutableParams;

    private long minTimestamp;

    private long maxTimestamp;

    private long eventCount;

    private int treeRootParentLevel;

    private long treeRootId;

    public PrimaryIndexMetaData(final EventSchema schema, final TimeRepresentation timeRepresentation,
                                final ImmutableParams ip, final MutableParams mp) {
        this.schema = schema;
        this.timeRepresentation = timeRepresentation;
        this.immutableParams = ip;
        this.mutableParams = mp;
        this.minTimestamp = Long.MAX_VALUE;
        this.maxTimestamp = Long.MIN_VALUE;
        this.treeRootParentLevel = 0;
        this.treeRootId = -1;
    }

    public static PrimaryIndexMetaData load(final Path metaDataFile) throws IOException {
        return load(metaDataFile, null);
    }

    public static PrimaryIndexMetaData load(final Path metaDataFile, final MutableParams override) throws IOException {
        log.debug("Loading TABMetaData from {}", metaDataFile);

        try (DataInputStream din = new DataInputStream(Files.newInputStream(metaDataFile, StandardOpenOption.READ))) {

            din.readByte();

            final PrimaryIndexMetaData result = new PrimaryIndexMetaData(EventSchema.read(din), TimeRepresentation.valueOf(din.readUTF()),
                    ImmutableParams.load(din), MutableParams.load(din));

            result.minTimestamp = din.readLong();
            result.maxTimestamp = din.readLong();
            result.eventCount = din.readLong();

            result.treeRootId = din.readLong();
            result.treeRootParentLevel = din.readInt();

            if (override != null) {
                log.debug("Overriding mutable config parameters.");
                result.mutableParams = override;
            }

            log.debug("Finished loading TABMetaData from {}", metaDataFile);
            return result;
        }
    }

    public boolean isOutOfOrder(final Event event) {
        return event.getT1() < this.maxTimestamp;
    }

    public void updateTimestamps(final Event event) {
        if (event.getT1() >= this.maxTimestamp) {
            this.maxTimestamp = event.getT1();
        }
        if (event.getT1() < this.minTimestamp) {
            this.minTimestamp = event.getT1();
        }
    }

    public void setTimestamps(long min, long max) {
        this.minTimestamp = min;
        this.maxTimestamp = max;
    }

    public void increaseEventCount() {
        eventCount++;
    }

    public void setTreeRoot(final BPlusTree.IndexEntry root) {
        if (root != null) {
            setTreeRoot((Long) root.id(), root.parentLevel());
        } else {
            setTreeRoot(-1L, 0);
        }
    }

    private void setTreeRoot(final long id, final int parentLevel) {
        this.treeRootId = id;
        this.treeRootParentLevel = parentLevel;
    }

    public EventSchema getSchema() {
        return this.schema;
    }

    public TimeRepresentation getTimeRepresentation() {
        return this.timeRepresentation;
    }

    public long getMinTimestamp() {
        return this.minTimestamp;
    }

    public long getMaxTimestamp() {
        return this.maxTimestamp;
    }

    public long getEventCount() {
        return this.eventCount;
    }

    public void setEventCount(long value) {
        this.eventCount = value;
    }

    public int getTreeRootParentLevel() {
        return this.treeRootParentLevel;
    }

    public long getTreeRootId() {
        return this.treeRootId;
    }

    public MutableParams getMutableParams() {
        return this.mutableParams;
    }

    public void setMutableParams(MutableParams mutableParams) {
        this.mutableParams = mutableParams;
    }

    public ImmutableParams getImmutableParams() {
        return this.immutableParams;
    }

    @Override
    public String toString() {
        return String.format(
                "MetaData [schema: %s, timeRepresentation: %s, immutableParams: %s, mutableParams: %s, minTimestamp: %s, maxTimestamp: %s, treeRootParentLevel: %s, treeRootId: %s]",
                this.schema, this.timeRepresentation, this.immutableParams, this.mutableParams, this.minTimestamp,
                this.maxTimestamp, this.treeRootParentLevel, this.treeRootId);
    }

    public void persist(final Path file) throws IOException {
        log.debug("Writing MetaData to {}", file);

        try (DataOutputStream dout = new DataOutputStream(
                Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {

            dout.writeByte(CURRENT_VERSION);

            this.schema.write(dout);

            dout.writeUTF(this.timeRepresentation.name());

            this.immutableParams.persist(dout);
            this.mutableParams.persist(dout);

            dout.writeLong(this.minTimestamp);
            dout.writeLong(this.maxTimestamp);
            dout.writeLong(eventCount);

            dout.writeLong(this.treeRootId);
            dout.writeInt(this.treeRootParentLevel);

            log.debug("Finished writing TABMetaData to {}", file);
        }
    }
}
