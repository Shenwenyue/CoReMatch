package CoReMatch.db.core.secondaryindex;

import CoReMatch.db.DBRuntimeException;
import CoReMatch.db.core.primaryindex.impl.legacy.SchemaManager;
import CoReMatch.db.event.TID;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import xxl.core.collections.containers.Container;
import xxl.core.collections.containers.io.BufferedContainer;
import xxl.core.collections.containers.io.ConverterContainer;
import xxl.core.collections.containers.io.FCBlockFileContainer;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;
import xxl.core.functions.Functional;
import xxl.core.functions.Functions;
import xxl.core.indexStructures.BPlusTree;
import xxl.core.indexStructures.LogStructuredMergeBPlusTree;
import xxl.core.indexStructures.LogStructuredMergeBPlusTree.LSMMeta;
import xxl.core.indexStructures.LogStructuredMergeBPlusTree.SortOrder;
import xxl.core.indexStructures.Separator;
import xxl.core.indexStructures.separators.ComparableKeyRange;
import xxl.core.indexStructures.separators.ComparableSeparator;
import xxl.core.io.LRUBuffer;
import xxl.core.io.converters.MeasuredConverter;
import xxl.core.util.Pair;
import xxl.core.util.WrappingRuntimeException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

public class LSMCompositeTimeIndex<K1 extends Comparable<K1>, K2 extends Comparable<K2>>
        implements CompositeSecondaryTimeIndex<K1, K2> {

    private static final int CONTAINER_LRU_BUFFER_SIZE = 100;

    private final Path path;
    private final MeasuredConverter<K1> key1Converter;
    private final MeasuredConverter<K2> key2Converter;
    private final MeasuredConverter<CompositeKey<K1, K2>> compositeKeyConverter;
    private final String attribute1Name;
    private final String attribute2Name;
    private final int blockSize;

    private int column1;
    private int column2;

    public LogStructuredMergeBPlusTree<CompositeKey<K1, K2>, Pair<CompositeKey<K1, K2>, EventID>> lsm;

    public LSMCompositeTimeIndex(
            Path databasePath,
            EventSchema schema,
            String attribute1,
            String attribute2,
            final int blockSize) {
        this(databasePath, schema, attribute1, attribute2, blockSize, false);
    }

    @SuppressWarnings("unchecked")
    public LSMCompositeTimeIndex(
            Path databasePath,
            EventSchema schema,
            String attribute1,
            String attribute2,
            final int blockSize,
            final boolean directIO) {

        this.column1 = schema.getAttributeIndex(attribute1);
        this.column2 = schema.getAttributeIndex(attribute2);
        this.attribute1Name = schema.getAttribute(column1).getName();
        this.attribute2Name = schema.getAttribute(column2).getName();

        this.path = databasePath.resolve("lsm_composite_" + column1 + "_" + column2);

        @SuppressWarnings("unchecked")
        MeasuredConverter<K1> converter1 = (MeasuredConverter) SchemaManager.getObjectConverter(schema.byName(attribute1));
        @SuppressWarnings("unchecked")
        MeasuredConverter<K2> converter2 = (MeasuredConverter) SchemaManager.getObjectConverter(schema.byName(attribute2));
        this.key1Converter = converter1;
        this.key2Converter = converter2;

        this.compositeKeyConverter = CompositeKey.getConverter(key1Converter, key2Converter);

        this.blockSize = blockSize;

        MeasuredConverter<EventID> eventIdConverter = EventID.getConverter();

        java.util.function.Function<CompositeKey<K1, K2>, Separator> separatorGenerator =
            argument -> new ComparableSeparator(argument);

        BiFunction<CompositeKey<K1, K2>, CompositeKey<K1, K2>, BPlusTree.KeyRange> lsmKeyRangeGenerator =
            (argument0, argument1) -> new ComparableKeyRange(argument0, argument1);

        MeasuredConverter<Pair<CompositeKey<K1, K2>, EventID>> pairConverter =
                new MeasuredConverter<Pair<CompositeKey<K1, K2>, EventID>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getMaxObjectSize() {

                return compositeKeyConverter.getMaxObjectSize() + eventIdConverter.getMaxObjectSize();
            }

            @Override
            public Pair<CompositeKey<K1, K2>, EventID> read(DataInput dataInput,
                    Pair<CompositeKey<K1, K2>, EventID> object) throws IOException {
                CompositeKey<K1, K2> key = compositeKeyConverter.read(dataInput);
                EventID eid = eventIdConverter.read(dataInput);
                return new Pair<>(key, eid);
            }

            @Override
            public void write(DataOutput dataOutput, Pair<CompositeKey<K1, K2>, EventID> object) throws IOException {
                compositeKeyConverter.write(dataOutput, object.getElement1());
                eventIdConverter.write(dataOutput, object.getElement2());
            }
        };

        final Functional.UnaryFunction<Pair<CompositeKey<K1, K2>, EventID>, CompositeKey<K1, K2>> getKey =
                new Functional.UnaryFunction<Pair<CompositeKey<K1, K2>, EventID>, CompositeKey<K1, K2>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositeKey<K1, K2> invoke(Pair<CompositeKey<K1, K2>, EventID> arg) {
                return arg.getElement1();
            }
        };

        LogStructuredMergeBPlusTree.BPlusTreeConfigurationFactory<CompositeKey<K1, K2>,
                Pair<CompositeKey<K1, K2>, EventID>> bPlusTreeConfigurationFactory =
                new LogStructuredMergeBPlusTree.BPlusTreeConfigurationFactory<CompositeKey<K1, K2>,
                        Pair<CompositeKey<K1, K2>, EventID>>() {
            @Override
            public LogStructuredMergeBPlusTree<CompositeKey<K1, K2>,
                    Pair<CompositeKey<K1, K2>, EventID>>.BPlusTreeConfiguration apply(
                            LogStructuredMergeBPlusTree<CompositeKey<K1, K2>,
                                    Pair<CompositeKey<K1, K2>, EventID>> t) {
                return t.new BPlusTreeConfiguration(
                        compositeKeyConverter,
                        pairConverter,
                        new FunctionalFunction<>(Functions.toFunction(getKey)),
                        separatorGenerator,
                        lsmKeyRangeGenerator,
                        null,
                        SortOrder.BY_LEVEL).blockSize(blockSize);

            }
        };

        BiFunction<BPlusTree.NodeConverter, Integer, Container> containerFactory =
                getContainerFactory(path, blockSize, directIO, CONTAINER_LRU_BUFFER_SIZE);

        BiFunction<BPlusTree.NodeConverter, Integer, Container> containerLoader =
                getContainerLoader(path, directIO, CONTAINER_LRU_BUFFER_SIZE);

        if (Files.exists(path)) {
            System.out.println("Loading LSM Composite Index for (" + attribute1Name + ", " + attribute2Name + ")");
            try {
                LSMMeta<CompositeKey<K1, K2>> meta = LSMMeta.load(path, compositeKeyConverter);
                lsm = new LogStructuredMergeBPlusTree<>(meta, containerLoader, containerFactory,
                        bPlusTreeConfigurationFactory,
                        (long) Math.pow(2, SecondaryIndexParams.COLA_CHACHE_SIZE));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not load LSM composite state.", e);
            }
        } else {
            System.out.println("Creating new LSM Composite Index for (" + attribute1Name + ", " + attribute2Name + ")");
            try {
                Files.createDirectories(path);
                lsm = new LogStructuredMergeBPlusTree<>(containerFactory, bPlusTreeConfigurationFactory,
                        (long) Math.pow(2, SecondaryIndexParams.COLA_CHACHE_SIZE));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not create lsm composite directory.", e);
            }
        }
    }

    @Override
    public Cursor<Pair<CompositeKey<K1, K2>, EventID>> rangeQueryByID1(K1 minKey1, K1 maxKey1) {

        CompositeKey<K1, K2> minComposite = new CompositeKey<>(minKey1, null);
        CompositeKey<K1, K2> maxComposite = new CompositeKey<>(maxKey1, null);
        return Cursors.wrap(lsm.query(minComposite, maxComposite));
    }

    @Override
    public Cursor<Pair<CompositeKey<K1, K2>, EventID>> rangeQueryByCompositeKey(
            K1 minKey1, K1 maxKey1, K2 minKey2, K2 maxKey2) {
        CompositeKey<K1, K2> minComposite = new CompositeKey<>(minKey1, minKey2);
        CompositeKey<K1, K2> maxComposite = new CompositeKey<>(maxKey1, maxKey2);
        return Cursors.wrap(lsm.query(minComposite, maxComposite));
    }

    @Override
    public Cursor<Pair<CompositeKey<K1, K2>, EventID>> rangeQueryID(CompositeKey<K1, K2> minKey,
            CompositeKey<K1, K2> maxKey) {
        return Cursors.wrap(lsm.query(minKey, maxKey));
    }

    public Pair<CompositeKey<K1, K2>, EventID> exactMatchQuery(K1 key1, K2 key2) {
        CompositeKey<K1, K2> compositeKey = new CompositeKey<>(key1, key2);
        return exactMatchQueryID(compositeKey);
    }

    public Cursor<Pair<CompositeKey<K1, K2>, EventID>> prefixQuery(K1 key1) {

        throw new UnsupportedOperationException("Prefix query requires type-specific bounds");
    }

    @Override
    public double estimateIO(double selectivity) {
        double totalLeaves = 0;

        for (int i = 1; i <= lsm.getMaxLevel(); i++) {
            if (lsm.levelExists(i)) {
                double numEvents = lsm.getLevel0Size() * Math.pow(2, i - 1);
                totalLeaves += Math.ceil(numEvents / lsm.getNumberOfLeafEntries());
            }
        }
        return (long) Math.ceil(totalLeaves * selectivity) * blockSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public LogStructuredMergeBPlusTree<CompositeKey<K1, K2>, Pair<CompositeKey<K1, K2>, EventID>> getLSMTree() {
        return lsm;
    }

    protected BiFunction<BPlusTree.NodeConverter, Integer, Container> getContainerFactory(
            Path path, int blockSize, boolean directIO, int bufferSize) {
        return new BiFunction<BPlusTree.NodeConverter, Integer, Container>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Container apply(BPlusTree.NodeConverter converter, Integer number) {
                try {
                    return new BufferedContainer(
                            new ConverterContainer(
                                    new FCBlockFileContainer(path, "level_" + number, blockSize, directIO),
                                    converter
                            ),
                            new LRUBuffer(bufferSize)
                    );

                } catch (IOException e) {
                    throw new WrappingRuntimeException(e);
                }
            }
        };
    }

    protected BiFunction<BPlusTree.NodeConverter, Integer, Container> getContainerLoader(
            Path path, boolean directIO, int bufferSize) {
        return new BiFunction<BPlusTree.NodeConverter, Integer, Container>() {
            @SuppressWarnings("rawtypes")
            @Override
            public Container apply(BPlusTree.NodeConverter converter, Integer number) {
                try {
                    return new BufferedContainer(
                            new ConverterContainer(
                                    new FCBlockFileContainer(path, "level_" + number, directIO),
                                    converter
                            ),
                            new LRUBuffer(bufferSize)
                    );
                } catch (IOException e) {
                    throw new WrappingRuntimeException(e);
                }
            }
        };
    }

    @Override
    public void close() {
        LSMMeta<CompositeKey<K1, K2>> meta = lsm.close();
        try {
            meta.write(path, compositeKeyConverter);
        } catch (IOException e) {
            throw new DBRuntimeException("Could not persist LSM composite state.", e);
        }
    }

    @Override
    public void insertEvent(Event event, TID tid, long sequenceId) {
        @SuppressWarnings("unchecked")
        K1 value1 = (K1) event.get(column1);
        @SuppressWarnings("unchecked")
        K2 value2 = (K2) event.get(column2);

        CompositeKey<K1, K2> compositeKey = new CompositeKey<>(value1, value2);
        EventID eventId = new EventID(tid.getBlockId(), tid.getOffset(), event.getT1(), sequenceId);

        lsm.insert(new Pair<>(compositeKey, eventId));
    }

    @Override
    public void build() {
        throw new UnsupportedOperationException("Build not supported yet!");
    }

    @Override
    public void build(long startTime) {
        throw new UnsupportedOperationException("Build not supported yet!");
    }

    @Override
    public void build(long startTime, long endTime) {
        throw new UnsupportedOperationException("Build not supported yet!");
    }

    @Override
    public void discardEvents(long timeStamp) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Discard not supported yet!");
    }

    @Override
    public void destroy() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Destroy not supported yet!");
    }

    @Override
    public String getAttributeName() {
        return attribute1Name + "_" + attribute2Name;
    }

    public String getAttribute1Name() {
        return attribute1Name;
    }

    public String getAttribute2Name() {
        return attribute2Name;
    }

    @SuppressWarnings("deprecation")
    private static class FunctionalFunction<I, O> implements java.util.function.Function<I, O> {
        private xxl.core.functions.Function<I, O> function;

        public FunctionalFunction(xxl.core.functions.Function<I, O> function) {
            this.function = function;
        }

        @Override
        public O apply(I i) {
            return function.invoke(i);
        }
    }
}
