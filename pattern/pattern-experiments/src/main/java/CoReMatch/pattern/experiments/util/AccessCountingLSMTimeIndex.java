package CoReMatch.pattern.experiments.util;

import CoReMatch.db.core.IOMeasuring;
import CoReMatch.db.core.secondaryindex.LSMTimeIndex;
import CoReMatch.db.core.secondaryindex.EventID;
import CoReMatch.event.EventSchema;
import xxl.core.collections.containers.Container;
import xxl.core.collections.containers.io.BufferedContainer;
import xxl.core.collections.containers.io.ConverterContainer;
import xxl.core.collections.containers.io.FCBlockFileContainer;
import xxl.core.indexStructures.BPlusTree;
import xxl.core.indexStructures.BPlusTree.NodeConverter;
import xxl.core.io.Buffer;
import xxl.core.io.LRUBuffer;
import xxl.core.util.Pair;
import xxl.core.util.WrappingRuntimeException;
import xxl.core.cursors.Cursor;
import xxl.core.cursors.Cursors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class AccessCountingLSMTimeIndex<K extends Comparable<K>> extends LSMTimeIndex<K> implements IOMeasuring {

    static final Map<AccessCountingLSMTimeIndex<?>, List<PageAccessCountingContainer>> counters = new HashMap<>();

    @SuppressWarnings("rawtypes")
    static final Map<AccessCountingLSMTimeIndex<?>, List<Pair<Container, Buffer>>> buffers = new HashMap<>();

    public AccessCountingLSMTimeIndex(Path databasePath, EventSchema schema, String attribute, int blockSize,
                                      boolean directIO) {
        super(databasePath, schema, attribute, blockSize, directIO);
    }

    public AccessCountingLSMTimeIndex(Path databasePath, EventSchema schema, String attribute, int blockSize) {
        super(databasePath, schema, attribute, blockSize);
    }

    public void resetMeasurement() {
        getCounters().forEach(x -> x.resetCounters());
    }

    public long getTotalReadBytes() {
        return getCounters().stream().map(x -> x.getReadCount() * getBlockSize()).collect(Collectors.summarizingLong(Long::valueOf)).getSum();
    }

    public long getTotalWrittenBytes() {
        return getCounters().stream().map(x -> x.getWriteCount() * getBlockSize()).collect(Collectors.summarizingLong(Long::valueOf)).getSum();
    }

    @SuppressWarnings("unchecked")
    public void emptyBuffers() {
        getBuffers().forEach(x -> x.getElement2().removeAll(x.getElement1()));
    }

    private List<PageAccessCountingContainer> getCounters() {
        var result = counters.get(this);
        if (result == null) {
            result = new ArrayList<>();
            counters.put(this, result);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private List<Pair<Container, Buffer>> getBuffers() {
        var result = buffers.get(this);
        if (result == null) {
            result = new ArrayList<>();
            buffers.put(this, result);
        }
        return result;
    }

    @Override
    protected BiFunction<NodeConverter, Integer, Container> getContainerFactory(Path path, int blockSize,
                                                                                boolean directIO, int bufferSize) {

        return new BiFunction<BPlusTree.NodeConverter, Integer, Container>() {

            @SuppressWarnings("rawtypes")
            @Override
            public Container apply(BPlusTree.NodeConverter converter, Integer number) {
                try {
                    Container plain = new FCBlockFileContainer(path, "level_" + number, blockSize, directIO);

                    PageAccessCountingContainer pacc = new PageAccessCountingContainer(
                            plain
                    );
                    getCounters().add(pacc);

                    Buffer b = new LRUBuffer(bufferSize);

                    Container buffered = new BufferedContainer(
                            new ConverterContainer(
                                    pacc,
                                    converter
                            ),
                            b
                    );

                    getBuffers().add(new Pair<>(buffered, b));

                    return buffered;

                } catch (IOException e) {
                    throw new WrappingRuntimeException(e);
                }
            }
        };

    }

    @Override
    protected BiFunction<NodeConverter, Integer, Container> getContainerLoader(Path path, boolean directIO, int bufferSize) {
        return new BiFunction<BPlusTree.NodeConverter, Integer, Container>() {

            @SuppressWarnings("rawtypes")
            @Override
            public Container apply(BPlusTree.NodeConverter converter, Integer number) {
                try {
                    Container plain = new FCBlockFileContainer(path, "level_" + number, directIO);

                    PageAccessCountingContainer pacc = new PageAccessCountingContainer(
                            plain
                    );

                    getCounters().add(pacc);

                    Buffer b = new LRUBuffer(bufferSize);

                    Container buffered = new BufferedContainer(
                            new ConverterContainer(
                                    pacc,
                                    converter
                            ),
                            b
                    );

                    getBuffers().add(new Pair<>(buffered, b));
                    return buffered;
                } catch (IOException e) {
                    throw new WrappingRuntimeException(e);
                }
            }
        };
    }

    public Map<K, Integer> getAllUniqueKeysWithCounts() {
        Map<K, Integer> keyCounts = new HashMap<>();

        Iterator<Pair<K, EventID>> allEntries = this.getLSMTree().query();

        while (allEntries.hasNext()) {
            Pair<K, EventID> entry = allEntries.next();
            K key = entry.getElement1();

            keyCounts.put(key, keyCounts.getOrDefault(key, 0) + 1);
        }

        return keyCounts;
    }

    public Set<K> getAllUniqueKeys() {
        Set<K> uniqueKeys = new HashSet<>();

        Iterator<Pair<K, EventID>> allEntries = this.getLSMTree().query();

        while (allEntries.hasNext()) {
            Pair<K, EventID> entry = allEntries.next();
            K key = entry.getElement1();
            uniqueKeys.add(key);
        }

        return uniqueKeys;
    }
}
