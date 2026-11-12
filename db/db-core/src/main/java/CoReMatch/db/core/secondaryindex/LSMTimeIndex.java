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
import java.util.Optional;
import java.util.function.BiFunction;

public class LSMTimeIndex<K extends Comparable<K>> implements SecondaryTimeIndex<K> {

    private static final int CONTAINER_LRU_BUFFER_SIZE = 100;
    private final Path path;
    private final MeasuredConverter<K> keyConverter;
    private final String attributeName;
    private final int blockSize;

    public LogStructuredMergeBPlusTree<K, Pair<K, EventID>> lsm;

    private int column;

    public LSMTimeIndex(
            Path databasePath,
            EventSchema schema,
            String attribute,
            final int blockSize) {
        this(databasePath, schema, attribute, blockSize, false);
    }

    @SuppressWarnings("unchecked")
    public LSMTimeIndex(
            Path databasePath,
            EventSchema schema,
            String attribute,
            final int blockSize,
            final boolean directIO) {

        this.column = schema.getAttributeIndex(attribute);
        this.attributeName = schema.getAttribute(column).getName();
        this.path = databasePath.resolve("lsm_column_" + column);
        this.keyConverter = (MeasuredConverter) SchemaManager.getObjectConverter(schema.byName(attribute));
        this.blockSize = blockSize;
        MeasuredConverter<EventID> eventIdConverter = EventID.getConverter();

        java.util.function.Function<K, Separator> separatorGenerator = new java.util.function.Function<K, Separator>() {

            @Override
            public Separator apply(K argument) {
                return new ComparableSeparator(argument);
            }

        };

        BiFunction<K, K, BPlusTree.KeyRange> lsmKeyRangeGenerator = new BiFunction<K, K, BPlusTree.KeyRange>() {
            @Override
            public BPlusTree.KeyRange apply(K argument0, K argument1) {
                return new ComparableKeyRange(argument0, argument1);

            }
        };

        MeasuredConverter<Pair<K, EventID>> pairConverter = new MeasuredConverter<Pair<K, EventID>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public int getMaxObjectSize() {
                return keyConverter.getMaxObjectSize() + eventIdConverter.getMaxObjectSize();
            }

            @Override
            public Pair<K, EventID> read(DataInput dataInput, Pair<K, EventID> object) throws IOException {
                return new Pair<>(keyConverter.read(dataInput), eventIdConverter.read(dataInput));
            }

            @Override
            public void write(DataOutput dataOutput, Pair<K, EventID> object) throws IOException {
                keyConverter.write(dataOutput, object.getElement1());
                eventIdConverter.write(dataOutput, object.getElement2());
            }
        };

        final Functional.UnaryFunction<Pair<K, EventID>, K> getKey = new Functional.UnaryFunction<Pair<K, EventID>, K>() {

            private static final long serialVersionUID = 1L;

            @Override
            public K invoke(Pair<K, EventID> arg) {
                return arg.getElement1();
            }
        };

        LogStructuredMergeBPlusTree.BPlusTreeConfigurationFactory<K, Pair<K, EventID>> bPlusTreeConfigurationFactory = new LogStructuredMergeBPlusTree.BPlusTreeConfigurationFactory<K, Pair<K, EventID>>() {
            @Override
            public LogStructuredMergeBPlusTree<K, Pair<K, EventID>>.BPlusTreeConfiguration apply(LogStructuredMergeBPlusTree<K, Pair<K, EventID>> t) {

                return t.new BPlusTreeConfiguration(
                        keyConverter,
                        pairConverter,
                        new FunctionalFunction<>(Functions.toFunction(getKey)),
                        separatorGenerator,
                        lsmKeyRangeGenerator,
                        null,
                        SortOrder.BY_LEVEL).blockSize(blockSize);
            }
        };

        BiFunction<BPlusTree.NodeConverter, Integer, Container> containerFactory = getContainerFactory(path, blockSize, directIO, CONTAINER_LRU_BUFFER_SIZE);

        BiFunction<BPlusTree.NodeConverter, Integer, Container> containerLoader = getContainerLoader(path, directIO, CONTAINER_LRU_BUFFER_SIZE);

        if (Files.exists(path)) {
            System.out.println("Loading LSM Index");
            try {
                LSMMeta<K> meta = LSMMeta.load(path, keyConverter);
                lsm = new LogStructuredMergeBPlusTree<>(meta, containerLoader, containerFactory, bPlusTreeConfigurationFactory, (long) Math.pow(2, SecondaryIndexParams.COLA_CHACHE_SIZE));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not load LSM state.", e);
            }
        } else {
            System.out.println("Creating new LSM Index");
            try {
                Files.createDirectories(path);
                lsm = new LogStructuredMergeBPlusTree<>(containerFactory, bPlusTreeConfigurationFactory, (long) Math.pow(2, SecondaryIndexParams.COLA_CHACHE_SIZE));
            } catch (IOException e) {
                throw new DBRuntimeException("Could not create lsm directory.", e);
            }

        }
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

    public Optional<BPlusTree> getTree(int level) {
        return lsm.getTree(level);
    }

    public int getBlockSize() {
        return blockSize;
    }

    protected LogStructuredMergeBPlusTree<K, Pair<K, EventID>> getLSMTree() {
        return lsm;
    }

    protected BiFunction<BPlusTree.NodeConverter, Integer, Container> getContainerFactory(Path path, int blockSize, boolean directIO, int bufferSize) {
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

    protected BiFunction<BPlusTree.NodeConverter, Integer, Container> getContainerLoader(Path path, boolean directIO, int bufferSize) {
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
        LSMMeta<K> meta = lsm.close();
        try {
            meta.write(path, keyConverter);
        } catch (IOException e) {
            throw new DBRuntimeException("Could not persists LSM state.", e);
        }
    }

    @Override
    public Cursor<Pair<K, EventID>> rangeQueryID(K minKey, K maxKey) {

        return Cursors.wrap(lsm.query(minKey, maxKey));

    }

    @SuppressWarnings("unchecked")
    @Override
    public void insertEvent(Event event, TID tid, long sequenceId) {

        K value = (K) event.get(column);
        lsm.insert(new Pair<>(value, new EventID(tid.getBlockId(), tid.getOffset(), event.getT1(), sequenceId)));

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
        return attributeName;
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
