package CoReMatch.db.core.primaryindex.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import CoReMatch.db.DBException;
import CoReMatch.db.DBRuntimeException;
import CoReMatch.db.core.primaryindex.HistogramAccess;
import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.core.primaryindex.impl.legacy.*;
import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.DoubleAttributeRange;
import CoReMatch.db.core.wal.Log;
import CoReMatch.db.core.wal.NoLog;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.*;
import CoReMatch.event.Attribute.DataType;
import xxl.core.collections.containers.AbstractContainer;
import xxl.core.collections.containers.Container;
import xxl.core.collections.containers.compression.Compressor;
import xxl.core.collections.containers.compression.FastCompressionContainer;
import xxl.core.collections.containers.compression.LZ4Compressor;
import xxl.core.collections.containers.compression.NoCompressor;
import xxl.core.collections.containers.io.FCBlockFileContainer;
import xxl.core.collections.containers.io.SuspendableContainer;
import xxl.core.collections.containers.io.SuspendableConverterContainer;
import xxl.core.cursor.DoubleCursor;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.filters.Filter;
import xxl.core.functions.AbstractFunction;
import xxl.core.functions.Functional.BinaryFunction;
import xxl.core.indexStructures.AggregateIndex.Aggregation;
import xxl.core.indexStructures.BPlusLink;
import xxl.core.indexStructures.BPlusLink.Node;
import xxl.core.indexStructures.FastAggregatedAppender;
import xxl.core.indexStructures.FastAggregatedBPlusTree;
import xxl.core.indexStructures.FastAggregatedBPlusTree.IndexEntry;
import xxl.core.io.converters.Converter;
import xxl.core.io.converters.Converters.SerializationMode;
import xxl.core.io.converters.FixedSizeConverter;
import xxl.core.io.converters.LongConverter;
import xxl.core.io.converters.MeasuredConverter;
import xxl.core.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public class PrimaryIndexImpl implements Closeable, AutoCloseable, PrimaryIndex {

    public static final Attribute TSTART =
            new Attribute("tstart", DataType.LONG);

    ;

    public static final Attribute TEND =
            new Attribute("tend", DataType.LONG);

    public static final String CONTAINER_FILE = "data";

    public static final String META_FILENAME = "meta.mf";

    public static final String META_FILENAME_TMP = "meta.mf.tmp";

    static final Logger log = LoggerFactory
            .getLogger(PrimaryIndexImpl.class);

    private static final MeasuredConverter<Long> KEY_CONVERTER =
            new MeasuredConverter<Long>() {

                private static final long serialVersionUID = 1L;

                @Override
                public int getMaxObjectSize() {
                    return 8;
                }

                @Override
                public Long read(
                        final DataInput dataInput,
                        final Long timestamp)
                        throws IOException {
                    return LongConverter.DEFAULT_INSTANCE
                            .readLong(
                                    dataInput);
                }

                @Override
                public void write(
                        final DataOutput dataOutput,
                        final Long timestamp)
                        throws IOException {
                    LongConverter.DEFAULT_INSTANCE
                            .writeLong(
                                    dataOutput,
                                    timestamp);
                }
            };

    private static final Function<Event, Long> KEY_FUNCTION =
            new Function<Event, Long>() {

                @Override
                public Long apply(
                        final Event event) {
                    return event.getT1();
                }
            };

    private static final BinaryFunction<TID, Event, PersistentEvent> GENERATE_OUT =
            new BinaryFunction<TID, Event, PersistentEvent>() {

                private static final long serialVersionUID =
                        1L;

                @Override
                public PersistentEvent invoke(
                        TID id,
                        Event event) {
                    return new PersistentEvent(
                            id, event);
                }
            };

    private final Path storageDirectory;

    PrimaryIndexMetaData metaData;

    FastAggregatedBPlusTree tree;

    FastAggregatedAppender<Event, PersistentEvent> appender;
    SuspendableContainer container;

    private State state = State.OPENING;

    public PrimaryIndexImpl(final Path storageDirectory) throws DBException {
        this(storageDirectory, null);
    }

    public PrimaryIndexImpl(final Path storageDirectory, MutableParams cfg) throws DBException {
        this.storageDirectory = storageDirectory.normalize();

        log.info("Loading TABPlusTree from: {}.", this.storageDirectory);

        final Path tmpMetaFile = this.storageDirectory.resolve(META_FILENAME_TMP);
        final Path metaFile = this.storageDirectory.resolve(META_FILENAME);

        try {

            if (Files.exists(tmpMetaFile)) {
                state = State.RECOVERING;
                loadDirty(tmpMetaFile, cfg);

                this.metaData.persist(tmpMetaFile);
            }

            else if (Files.exists(metaFile)) {
                loadClean(metaFile, cfg);

                this.metaData.persist(tmpMetaFile);
            } else
                throw new FileNotFoundException(String.format("Meta data file not found (%s)", metaFile));
        } catch (IOException cause) {
            throw new LoadException(
                    String.format("Could not load TAB+-Tree from directory: %s.", storageDirectory),
                    cause);
        }

        state = State.OPEN;
        log.info("Finished loading TABPlusTree from: " +
                        "\n  Storage-Directory   : {}" +
                        "\n  MetaData            : {}",
                this.storageDirectory, this.metaData);
        log.info(
                "Tree Properties:" +
                        "\n  EventsPerLeaf      : [{},{}]" +
                        "\n  IndexEntriesPerNode: [{},{}]",
                tree.getLeafNodeD(), tree.getLeafNodeB(),
                tree.getIndexNodeD(), tree.getIndexNodeB());
        log.info(
                "Appender Properties: " +
                        "\n  MaxLeafEntries : {}" +
                        "\n  maxIndexEntries: {}",
                appender.getMaxLeafEntries(), appender.getMaxIndexEntries()
        );
        log.info("Data Properties: " +
                "\n  Event count  : {}" +
                "\n  Time interval: {}", getNumberOfEvents(), getCoveredTimeInterval());
    }

    public PrimaryIndexImpl(final Path storageDirectory, final EventSchema schema,
                            final TimeRepresentation timeRepresentation, final ImmutableParams ip, final MutableParams mp)
            throws DBException {

        this.storageDirectory = storageDirectory.normalize();

        this.metaData = new PrimaryIndexMetaData(schema, timeRepresentation, ip, mp);

        log.info(
                "Creating new TABPlusTree: " +
                        "\n  Storage-Directory   : {}" +
                        "\n  Schema              : {}" +
                        "\n  Time-Represenstation: {}" +
                        "\n  ImmutableConfig     : {}" +
                        "\n  MutableConfig       : {}",
                this.storageDirectory, schema, timeRepresentation, ip, mp);

        final Path tmpMetaFile = this.storageDirectory.resolve(META_FILENAME_TMP);
        final Path metaFile = this.storageDirectory.resolve(META_FILENAME);

        try {
            if (!Files.exists(this.storageDirectory)) {
                log.debug("Storage-Directory does not exist. Creating.");
                Files.createDirectories(this.storageDirectory);
            } else if (Files.exists(metaFile) || Files.exists(tmpMetaFile)) {
                log.error("Found MetaData-File in storage directory. Aborting.");
                throw new InitialializationException(
                        String.format("There already is a TABPlusTree in %s.", this.storageDirectory.toString()));
            }

            this.metaData.persist(tmpMetaFile);

        } catch (IOException cause) {
            throw new InitialializationException(
                    String.format("Could not create TAB+-Tree in directory: %s.", storageDirectory),
                    cause);
        }

        final Log<Event> WAL = new NoLog<>();
        final Log<Event> OOLog = new NoLog<>();

        this.tree = createTree(WAL);
        this.appender = createAppender(this.tree, WAL, OOLog);

        state = State.OPEN;

        log.info(
                "Tree Properties:" +
                        "\n  EventsPerLeaf      : [{},{}]" +
                        "\n  IndexEntriesPerNode: [{},{}]",
                tree.getLeafNodeD(), tree.getLeafNodeB(),
                tree.getIndexNodeD(), tree.getIndexNodeB());
        log.info(
                "Appender Properties: " +
                        "\n  MaxLeafEntries : {}" +
                        "\n  maxIndexEntries: {}",
                appender.getMaxLeafEntries(), appender.getMaxIndexEntries());
        log.info("Finished creating new TABPlusTree at: {}.", this.storageDirectory);
    }

    private static boolean intersectsAll(IndexEntry e, List<? extends AttributeRange<? extends Number>> ranges) {
        return ranges.stream().allMatch(x -> intersectsSingle(e, x));
    }

    private static boolean intersectsSingle(IndexEntry e, AttributeRange<? extends Number> range) {
        var agg = e.getAggregation(range.getName());
        var idxRange = new DoubleAttributeRange(range.getName(), agg.getValues()[1].doubleValue(),
                agg.getValues()[2].doubleValue(), true, true);
        var givenRange = new DoubleAttributeRange(range.getName(), range.getLower().doubleValue(),
                range.getUpper().doubleValue(), range.isLowerInclusive(), range.isUpperInclusive());
        return givenRange.intersects(idxRange);
    }

    @Override
    public TimeInterval getCoveredTimeInterval() {
        return new TimeInterval(this.metaData.getMinTimestamp(), this.metaData.getMaxTimestamp());
    }

    @Override
    public int getMaxLeafEntries() {
        return appender.getMaxLeafEntries();
    }

    @Override
    public int getMaxIndexEntries() {
        return appender.getMaxIndexEntries();
    }

    @Override
    public long getNumberOfEvents() {
        return this.metaData.getEventCount();
    }

    @Override
    public int getHeight() {
        ensureState(State.OPEN);
        return appender.getTempRoot(false).getElement1().level();
    }

    private long computeEventCount() {
        Attribute indexed = null;
        for (Attribute a : this.metaData.getSchema()) {
            if (a.getProperty("index") != null && Boolean.valueOf(a.getProperty("index"))) {
                indexed = a;
                break;
            }
        }

        if (indexed != null) {
            return this.tree.getAggregate(
                    indexed.getName(),
                    Long.MIN_VALUE,
                    Long.MAX_VALUE,
                    (FastAggregatedBPlusTree.Node) this.appender.getTempRoot(true).getSecond(),
                    this.metaData.getMinTimestamp()).getValues()[3].longValue();
        } else {
            long count = 0L;
            Cursor<PersistentEvent> c = this.appender.query(Long.MIN_VALUE, Long.MAX_VALUE);
            try {
                c.open();
                while (c.hasNext()) {
                    count++;
                    c.next();
                }
                c.close();
                return count;
            } finally {
                try {
                    c.close();
                } catch (Exception e) {
                }
            }
        }

    }

    @Override
    public long getSerializedEventSize() {
        return SchemaManager.calculateEventSize(getSchema(), metaData.getTimeRepresentation());
    }

    @Override
    public long getContainerSize() {
        try {
            return Files.size(storageDirectory.resolve("data.ctr"));
        } catch (IOException e) {
            log.error("Unable to determine container size.", e);
        }
        return 0;
    }

    @Override
    public EventSchema getSchema() {
        return this.metaData.getSchema();
    }

    @Override
    public Future<TID> insert(final Event event) {
        ensureState(State.OPEN);
        Future<TID> result;

        if (log.isTraceEnabled()) {
            log.trace("Inserting event: {}", event);
        }

        if (this.metaData.isOutOfOrder(event)) {
            result = this.appender.outOfOrderInsert(event);
        } else {
            result = this.appender.insertEntry(event);
        }

        this.metaData.updateTimestamps(event);
        this.metaData.increaseEventCount();
        return result;
    }

    @Override
    public DoubleCursor<PersistentEvent> find(TID id) throws NoSuchEventException {
        ensureState(State.OPEN);
        return appender.find(id);
    }

    @Override
    public DoubleCursor<PersistentEvent> query(final long minKey, final long maxKey) {
        ensureState(State.OPEN);
        long time = 0L;
        if (log.isDebugEnabled()) {
            time = -System.currentTimeMillis();
            log.debug("Executing range query: [{}, {}]", minKey, maxKey);
        }

        final DoubleCursor<PersistentEvent> queryResult = this.appender.query(minKey, maxKey);

        if (log.isDebugEnabled()) {
            log.debug("Finished executing range query: [{}, {}]. Execution time: {} ms", minKey, maxKey,
                    (time + System.currentTimeMillis()));
        }
        return queryResult;
    }

    public Cursor<Cursor<PersistentEvent>> query(Cursor<TimeInterval> intervals) {

        ensureState(State.OPEN);
        long time = 0L;
        if (log.isDebugEnabled()) {
            time = -System.currentTimeMillis();
            log.debug("Executing multi interval query");
        }
        var root = appender.getTempRoot(true);

        final Cursor<Cursor<PersistentEvent>> queryResult =
                new MultiIntervalCursor(intervals, metaData.getMinTimestamp(), root);
        if (log.isDebugEnabled()) {
            log.debug("Finished executing multi interval query. Execution time: {} ms",
                    (time + System.currentTimeMillis()));
        }
        return queryResult;

    }

    @SuppressWarnings("unchecked")
    @Override
    public Cursor<PersistentEvent> querySecondaryAttributes(final long minKey, final long maxKey,
                                                            final AttributeRange<?>... ranges)
            throws SchemaException {
        ensureState(State.OPEN);
        long time = 0L;
        if (log.isDebugEnabled()) {
            time = -System.currentTimeMillis();
            log.debug("Executing secondary attribute query. Key-range: [{}, {}], ranges: {}", minKey, maxKey,
                    Arrays.toString(ranges));
        }

        if (ranges.length == 0)
            return query(minKey, maxKey);

        for (final AttributeRange<?> range : ranges) {
            final Attribute attr = this.metaData.getSchema().byName(range.getName());
            if (attr.getType() != range.getDataType())
                throw new SchemaException(String.format(
                        "Incompatible data type for attribute \"%s\". Schema type is %s, but range type is %s",
                        attr.getName(), attr.getType(), range.getDataType()));

        }

        final LongKeyRange keys = new LongKeyRange(minKey, maxKey);
        final Map<String, AttributeRange<? extends Number>> transformed = new HashMap<>();

        for (final AttributeRange<?> r : ranges) {
            if (r.getDataType().isNumeric())
                transformed.put(r.getName(), (AttributeRange<? extends Number>) r);
            else
                log.warn("Range {} not supported.", r);
        }

        final Cursor<PersistentEvent> queryResult = this.appender.queryAttributes(keys, transformed);

        final AttributeRange.EventMatcher[] matchers = new AttributeRange.EventMatcher[ranges.length];
        for (int i = 0; i < ranges.length; i++) {
            matchers[i] = ranges[i].getMatcher(getSchema());
        }

        final Cursor<PersistentEvent> filtered =
                new Filter<>(queryResult, new xxl.core.functions.AbstractFunction<PersistentEvent, Boolean>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean invoke(final PersistentEvent argument) {
                        if (argument.getT1() < minKey || argument.getT1() > maxKey)
                            return false;

                        for (final AttributeRange.EventMatcher matcher : matchers) {
                            if (!matcher.matches(argument))
                                return false;
                        }
                        return true;
                    }
                });

        if (log.isDebugEnabled()) {
            log.debug(
                    "Finished executing secondary attribute query. Key-range: [{}, {}], ranges: {}. Execution time: {} ms",
                    minKey, maxKey,
                    Arrays.toString(ranges), (time + System.currentTimeMillis()));
        }

        return filtered;
    }

    @Override
    public Aggregation getAggregates(final String attribute, final long minKey, final long maxKey) {
        ensureState(State.OPEN);
        long time = 0L;
        Aggregation result = null;
        if (log.isDebugEnabled()) {
            time = -System.currentTimeMillis();
            log.debug("Executing aggregate query for attribute {}. Key-range: [{}, {}]", attribute, minKey, maxKey);
        }

        try {
            Attribute attr = this.metaData.getSchema().byName(attribute);

            if (this.appender.getFirst() == null)
                result = new FastAggregatedBPlusTree.AggregationEntry();
            else
                result = this.tree.getAggregate(
                        attr.getName(),
                        Long.valueOf(minKey),
                        Long.valueOf(maxKey),
                        (FastAggregatedBPlusTree.Node) this.appender.getTempRoot(true).getSecond(),
                        this.metaData.getMinTimestamp());
        } catch (final SchemaException se) {
            throw new IllegalArgumentException(se);
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished Executing aggregate query for attribute {}. Key-range: [{}, {}]", attribute, minKey,
                    maxKey,
                    (time + System.currentTimeMillis()));
        }

        return result;
    }

    public State getState() {
        return this.state;
    }

    @Override
    public void flush() {
        ensureState(State.OPEN);
        appender.flushPath();
    }

    @Override
    public void close() {
        if (state == State.CLOSED)
            return;
        ensureState(State.OPEN);

        log.info("Closing TABPlusTree at: {}", this.storageDirectory);

        log.debug("Flushing appender.");
        this.appender.flushPathAndClose();

        log.debug("Closing container.");
        this.tree.container().close();

        final Path tmpMetaFile = this.storageDirectory.resolve(META_FILENAME_TMP);
        final Path metaFile = this.storageDirectory.resolve(META_FILENAME);

        this.metaData.setTreeRoot(this.tree.rootEntry());

        log.debug("Writing state information.");
        try {
            this.metaData.persist(tmpMetaFile);

            Files.move(tmpMetaFile, metaFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            state = State.CLOSED;
        } catch (final IOException ioex) {
            log.error("Could not write state information to " + META_FILENAME + ". Some data might get lost.", ioex);
            state = State.ERROR;
        }
        log.info("Finished closing TABPlusTree at: {}", this.storageDirectory);
    }

    void ensureState(State state) {
        if (this.state != state)
            throw new IllegalStateException(
                    String.format("Illegal state for this operation. Required: %s, current: %s.", state, this.state));
    }

    private FastAggregatedBPlusTree createTree(final Log<Event> wal) {

        final FastAggregatedBPlusTree tree = new FastAggregatedBPlusTree(this.metaData.getSchema(),
                this.metaData.getTimeRepresentation(),
                this.metaData.getImmutableParams().getBlockSize(), true, wal);

        final Path path = this.storageDirectory.resolve(CONTAINER_FILE);

        container = createContainer(path, tree.nodeConverter());

        initializeEmptyTree(tree, container);
        return tree;
    }

    private void initializeEmptyTree(final FastAggregatedBPlusTree tree, final SuspendableContainer treeContainer) {
        tree.initialize(null, null, new AbstractFunction<Event, Long>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Long invoke(final Event argument) {
                        return PrimaryIndexImpl.KEY_FUNCTION.apply(argument);
                    }
                }, treeContainer, KEY_CONVERTER,
                SchemaManager.getDataConverter(metaData.getSchema(), metaData.getTimeRepresentation()),
                LongSeparator.FACTORY_FUNCTION,
                LongKeyRange.FACTORY_FUNCTION);

    }

    protected SuspendableContainer createContainer(final Path containerPath, final Converter<?> treeNodeConverter) {
        ImmutableParams ip = this.metaData.getImmutableParams();
        MutableParams mp = this.metaData.getMutableParams();

        SuspendableContainer raw = null;

        if (ip.getContainerType() == ImmutableParams.ContainerType.FCC) {

            Compressor comp = null;
            switch (ip.getCompression()) {
                case LZ4:
                    comp = new LZ4Compressor();
                    break;
                default:
                    comp = new NoCompressor();
                    break;
            }

            raw = new FastCompressionContainer(
                    containerPath.toString(),
                    ip.getMacroBlockSize(),
                    ip.getBlockSize(),
                    comp,
                    ip.getContainerSpare(),
                    mp.getMacroBlockBufferSize(),
                    mp.isUseDirectIO(),
                    mp.isUseBlockBuffer());

        } else if (ip.getContainerType() == ImmutableParams.ContainerType.BLOCK) {
            Path dir = containerPath.getParent();
            String name = containerPath.getFileName().toString();
            try {
                raw = new BFWrapper(new FCBlockFileContainer(dir, name, ip.getBlockSize(), mp.isUseDirectIO()));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not create container.", e);
            }
        }
        return new SuspendableConverterContainer(raw, treeNodeConverter, SerializationMode.UNSAFE,
                this.metaData.getImmutableParams().getBlockSize());
    }

    private void loadClean(final Path metaDataFile, MutableParams cfg) throws IOException {
        log.debug("Loading TAB+-Tree state from {}.", metaDataFile);
        this.metaData = PrimaryIndexMetaData.load(metaDataFile, cfg);
        log.debug("Tree meta data: {}", metaDataFile);

        final Log<Event> WAL = new NoLog<>();
        final Log<Event> OOLog = new NoLog<>();

        this.tree = loadTree(WAL);
        this.appender = createAppender(this.tree, WAL, OOLog);
    }

    private void loadDirty(final Path tmpMetaDataFile, MutableParams cfg) throws RecoveryException, IOException {
        log.info("Recovering TAB+-Tree state from dirty meta data at: {}", tmpMetaDataFile);
        this.metaData = PrimaryIndexMetaData.load(tmpMetaDataFile, cfg);

        log.debug("Tree meta data: {}", this.metaData);

        final Log<Event> WAL = new NoLog<>();
        final Log<Event> OOLog = new NoLog<>();

        final Pair<FastAggregatedBPlusTree, FastAggregatedAppender<Event, PersistentEvent>> recoveryResult =
                recover(WAL, OOLog);

        this.tree = recoveryResult.getFirst();
        this.appender = recoveryResult.getSecond();

        if (this.appender.getFirst() != null) {
            this.metaData.setTimestamps(this.appender.getFirst().getT1(), this.appender.getLast().getT1());

            this.metaData.setEventCount(computeEventCount());
        } else {
            this.metaData.setEventCount(0);
            this.metaData.setTimestamps(Long.MAX_VALUE, Long.MIN_VALUE);
        }

    }

    private FastAggregatedBPlusTree loadTree(final Log<Event> wal) {

        final FastAggregatedBPlusTree tree = new FastAggregatedBPlusTree(metaData.getSchema(),
                metaData.getTimeRepresentation(), this.metaData.getImmutableParams().getBlockSize(), true, wal);

        final Path path = this.storageDirectory.resolve(CONTAINER_FILE);
        container = loadContainer(path, tree.nodeConverter());

        initializeEmptyTree(tree, container);

        IndexEntry ie = null;
        LongKeyRange rootDescriptor = null;

        if (this.metaData.getTreeRootId() >= 0) {

            rootDescriptor = new LongKeyRange(this.metaData.getMinTimestamp(), this.metaData.getMaxTimestamp());
            ie = tree.createIndexEntry(this.metaData.getTreeRootParentLevel());
            ie.initialize(this.metaData.getTreeRootId(), new LongSeparator(this.metaData.getMaxTimestamp()));
        }

        tree.initialize(ie, rootDescriptor, new AbstractFunction<Event, Long>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Long invoke(final Event argument) {
                        return PrimaryIndexImpl.KEY_FUNCTION.apply(argument);
                    }
                }, container, KEY_CONVERTER,
                SchemaManager.getDataConverter(metaData.getSchema(), metaData.getTimeRepresentation()),
                LongSeparator.FACTORY_FUNCTION, LongKeyRange.FACTORY_FUNCTION);
        return tree;
    }

    protected SuspendableContainer loadContainer(final Path containerPath, final Converter<?> treeNodeConverter) {
        MutableParams mp = this.metaData.getMutableParams();
        ImmutableParams ip = this.metaData.getImmutableParams();

        SuspendableContainer raw = null;
        if (ip.getContainerType() == ImmutableParams.ContainerType.FCC) {
            raw = new FastCompressionContainer(
                    containerPath.toString(),
                    mp.getMacroBlockBufferSize(),
                    mp.isUseDirectIO(),
                    mp.isUseBlockBuffer());
        } else if (ip.getContainerType() == ImmutableParams.ContainerType.BLOCK) {
            Path dir = containerPath.getParent();
            String name = containerPath.getFileName().toString();
            try {
                raw = new BFWrapper(new FCBlockFileContainer(dir, name, mp.isUseDirectIO()));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not create container.", e);
            }
        }
        return new SuspendableConverterContainer(raw, treeNodeConverter, SerializationMode.UNSAFE,
                this.metaData.getImmutableParams().getBlockSize());
    }

    private FastAggregatedAppender<Event, PersistentEvent> createAppender(final FastAggregatedBPlusTree tree,
                                                                          final Log<Event> wal,
                                                                          final Log<Event> ooLog) {

        ImmutableParams ip = this.metaData.getImmutableParams();
        MutableParams mp = this.metaData.getMutableParams();

        return new FastAggregatedAppender<>(tree,
                mp.getLeafBufferNodes(), wal, ooLog,
                ip.getTreeSpare(),
                mp.getOutOfOrderQueueSize(),
                mp.getTreeBufferSize(), KEY_FUNCTION, GENERATE_OUT);
    }

    private Pair<FastAggregatedBPlusTree, FastAggregatedAppender<Event, PersistentEvent>> recover(final Log<Event> wal,
                                                                                                  final Log<Event> ooLog) throws RecoveryException {

        final FastAggregatedBPlusTree tree =
                new FastAggregatedBPlusTree(metaData.getSchema(), metaData.getTimeRepresentation(),
                        this.metaData.getImmutableParams().getBlockSize(), true, wal);

        final Path path = this.storageDirectory.resolve(CONTAINER_FILE);
        container = loadContainer(path, tree.nodeConverter());

        initializeEmptyTree(tree, container);

        PrimaryIndexRecovery.RecoveryResult recoveryResult;
        try {
            recoveryResult = PrimaryIndexRecovery.recoverTreePath(KEY_FUNCTION, container);
        } catch (IllegalStateException e) {
            log.error("Recovery failed. Falling back to brute force.", e);
            recoveryResult = PrimaryIndexRecovery.recoverTreePathWithBruceForce(KEY_FUNCTION, container);
        }

        switch (recoveryResult.getType()) {
            case EMPTY:
                log.debug("Container does not contain any data. Returning fresh tree.");
                break;
            case REGULAR_LOAD:
                log.debug("No recovery necessary, performing regular load.");
                break;
            case RECOVER: {
                SortedMap<Integer, PrimaryIndexRecovery.RNode> exportPath = recoveryResult.getTreePath();
                final BPlusLink.Node rightMostLeaf = exportPath.get(PrimaryIndexRecovery.LEAF_LEVEL).node;
                final Event last = (Event) rightMostLeaf.getLast();
                final Event first = getFirstEvent(container);
                final List<Pair<Long, ? extends Node>> appenderPath = new ArrayList<>();
                exportPath.entrySet().forEach(x -> appenderPath.add(new Pair<>(x.getValue().getId(), x.getValue().getNode())));
                final FastAggregatedAppender<Event, PersistentEvent> recoveryAppender =
                        new FastAggregatedAppender<>(tree, this.metaData.getImmutableParams().getTreeSpare(),
                                this.metaData.getMutableParams().getLeafBufferNodes(), GENERATE_OUT, appenderPath,
                                first, last);

                recoveryAppender.recover();
                recoveryAppender.flushPathAndClose();
            }
        }
        return new Pair<>(tree, createAppender(tree, wal, ooLog));
    }

    @Override
    public double getCompressionRatio() {
        return container.getCompressionRatio();
    }

    private Event getFirstEvent(final Container con) throws RecoveryException {
        @SuppressWarnings("unchecked")
        Iterator<Long> ids = con.ids();
        while (ids.hasNext()) {
            Node node = (Node) con.get(ids.next());
            if (node.level == 0 && node.previousNeighbor() == null)
                return (Event) node.getFirst();
        }
        throw new RecoveryException("Could not recover first entry. Leftmost leaf not found.");
    }

    public FastAggregatedBPlusTree getTree() {
        return tree;
    }

    public FastAggregatedAppender<Event, PersistentEvent> getAppender() {
        return appender;
    }

    @Override
    public long estimateIO(long numPages) {
        return (long) (numPages * metaData.getImmutableParams().getBlockSize());
    }

    @SuppressWarnings("unchecked")
    public void walkLevel(int level, Consumer<HistogramAccess> consumer) {
        ensureState(State.OPEN);
        var p = appender.getTempRoot(true);

        IndexEntry entry = (IndexEntry) p.getElement1();
        FastAggregatedBPlusTree.Node node = (FastAggregatedBPlusTree.Node) p.getElement2();

        if (level > p.getElement1().level() || level < 1)
            throw new IllegalArgumentException(
                    "Can only walk tree-levels between " + node.level + " (tree height) and 1");

        while (node.level > level) {
            IndexEntry tmp = (IndexEntry) node.getEntry(0);
            entry.unfix();
            entry = tmp;
            node = entry.get(false);
        }

        Iterator<IndexEntry> entryIter = node.entries();

        AtomicLong minTS = new AtomicLong(metaData.getMinTimestamp());
        AtomicLong counter = new AtomicLong(0);
        while (entryIter.hasNext() || node.nextNeighbor != null) {
            if (entryIter.hasNext()) {
                final IndexEntry current = entryIter.next();
                final Long maxTS = (Long) current.separator().sepValue();
                final TimeInterval interval = new TimeInterval(minTS.get(), maxTS);
                minTS.set(maxTS + 1);

                consumer.accept(new HistogramAccess() {

                    final long index = counter.getAndIncrement();

                    public long getIndex() {
                        return index;
                    }

                    @Override
                    public Aggregation getAggregates(String attribute) {
                        return current.getAggregation(attribute);
                    }

                    @Override
                    public TimeInterval getCoveredTimeInterval() {
                        return interval;
                    }

                    @Override
                    public boolean intersects(List<? extends AttributeRange<? extends Number>> ranges) {
                        return intersectsAll(current, ranges);
                    }

                    @Override
                    public long getEventCount() {
                        for (var a : metaData.getSchema())
                            if (a.getProperty("index") != null && a.getProperty("index").equalsIgnoreCase("true"))
                                return current.getAggregation(a.getName()).getValues()[3].longValue();
                        throw new SchemaException("No attribute is indexed, cannot count events.");
                    }

                    @Override
                    public boolean intersects(AttributeRange<? extends Number> range) {
                        return intersectsSingle(current, range);
                    }

                    @Override
                    public String toString() {
                        return current.plot();
                    }

                });
            } else {
                entry.unfix();
                entry = (IndexEntry) node.nextNeighbor;
                node = entry.get(false);
                entryIter = node.entries();
            }
        }
    }

    public static enum State {
        OPENING, RECOVERING, OPEN, CLOSED, ERROR
    }

    static class BFWrapper extends AbstractContainer implements SuspendableContainer {
        final FCBlockFileContainer src;

        public BFWrapper(FCBlockFileContainer src) {
            this.src = src;
        }

        private Object convertIdIn(Object id) {
            return ((Long) id) * src.blockSize();
        }

        private Object convertIdOut(Object id) {
            return ((Long) id) / src.blockSize();
        }

        @Override
        public Object get(Object id, boolean unfix) throws NoSuchElementException {
            return src.get(convertIdIn(id));
        }

        @Override
        public Iterator ids() {
            Iterator src = this.src.ids();
            return new Iterator() {
                @Override
                public boolean hasNext() {
                    return src.hasNext();
                }

                @Override
                public Object next() {
                    return convertIdOut(src.next());
                }
            };
        }

        @Override
        public void close() {
            super.close();
            src.close();
        }

        @Override
        public boolean isUsed(Object id) {
            return src.isUsed(convertIdIn(id));
        }

        @Override
        public FixedSizeConverter objectIdConverter() {
            return src.objectIdConverter();
        }

        @Override
        public void remove(Object id) throws NoSuchElementException {
            src.remove(convertIdIn(id));
        }

        @Override
        public Object reserve(xxl.core.functions.Function getObject) {
            return convertIdOut(src.reserve(getObject));
        }

        @Override
        public int size() {
            return src.size();
        }

        @Override
        public void update(Object id, Object object, boolean unfix) throws NoSuchElementException {
            src.update(convertIdIn(id), object, unfix);
        }

        @Override
        public void suspend() {
        }

        @Override
        public void resume() {
        }

        @Override
        public boolean isSuspended() {
            return false;
        }

        @Override
        public Iterator<?> idsBackwards() {
            Deque result = new ArrayDeque(size());
            ids().forEachRemaining(result::addFirst);
            return result.iterator();
        }

        @Override
        public double getCompressionRatio() {
            return 1;
        }

        @Override
        public long getAllocatedSpace() {
            return (long) size() * src.blockSize();
        }

        @Override
        public void allocateSpace(long space) {
        }
    }
}
