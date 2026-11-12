package xxl.core.indexStructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CoReMatch.db.core.primaryindex.impl.InsertFuture;
import CoReMatch.db.core.wal.Log;
import CoReMatch.db.core.wal.NoLog;
import CoReMatch.db.event.Persistent;
import CoReMatch.db.event.TID;
import CoReMatch.db.queries.NoSuchEventException;
import xxl.core.collections.MapEntry;
import xxl.core.collections.containers.AbstractContainer;
import xxl.core.collections.containers.Container;
import xxl.core.cursor.DoubleCursor;
import xxl.core.cursor.EmptyDoubleCursor;
import xxl.core.functions.AbstractFunction;
import xxl.core.functions.Function;
import xxl.core.functions.Functional.BinaryFunction;
import xxl.core.indexStructures.BPlusLink.IndexEntry;
import xxl.core.indexStructures.BPlusLink.Node;
import xxl.core.indexStructures.appender.LeafLevelCursor;
import xxl.core.io.Buffer;
import xxl.core.io.LRUBuffer;
import xxl.core.io.converters.FixedSizeConverter;
import xxl.core.util.Pair;

@SuppressWarnings("deprecation")
public abstract class FastAppender<TIN, TOUT extends Persistent<TIN>> implements Appender<TIN,TOUT> {

	private static final Logger	logger		= LoggerFactory.getLogger(FastAppender.class);

	protected static final int	LEAF_LEVEL	= 0;

	protected class BufferEntry {

		long	id;
		Node	node;
		Long 	nextId;

		public BufferEntry(final long id, final Node node) {
			this(id, node, null);
		}

		public BufferEntry(final long id, final Node node, final Long nextId) {
			this.node = node;
			this.nextId = nextId;
			this.id = id;
		}

		@Override
		public String toString() {
			return this.node.toString();
		}
	}

	protected class BufferWriteEntry extends BufferEntry {

		public BufferWriteEntry(final long id, final Node node) {
			this(id, node, null);
		}

		public BufferWriteEntry(final long id, final Node node, final Long nextId) {
			super(id,node,nextId);
		}

	}

	private int										treeSize;

	protected AppenderBuffer						buffer;

	protected AppenderContainer						appenderContainer;

	protected BPlusLink								tree;

	protected int									maxLeafEntries;

	protected int									maxIndexEntries;

	private TIN										first;

	private TIN										last;

	protected ReentrantLock							bufferLock;

	protected Log<TIN> wal;

	protected Log<TIN>								outOfOrderQueueDiskMirror;

	protected Queue<TIN>								outOfOrderQueue;

	protected int									outOfOrderQueueSize;

	protected int									treeBufferSize;

	protected java.util.function.Function<TIN, Long>	keyFunction;

	protected final BinaryFunction<TID, TIN, TOUT>		generateOut;

	public FastAppender(final BPlusLink tree, final int bufferSize, final int idStepSize, final Log<TIN> log,
			final Log<TIN> outOfOrderQueueDiskMirror, final float spare, final int outOfOrderQueueSize,
			final int treeBufferSize, final java.util.function.Function<TIN, Long> getKeyFunction, BinaryFunction<TID,TIN,TOUT> generateOut) {
		this(tree, bufferSize, idStepSize, treeBufferSize, generateOut);
		this.wal = log;
		this.outOfOrderQueueDiskMirror = outOfOrderQueueDiskMirror;
		this.maxIndexEntries = (int) (tree.getIndexNodeB() * (1.0f - spare));
		this.maxLeafEntries = (int) (tree.getLeafNodeB() * (1.0f - spare));
		this.outOfOrderQueueSize = outOfOrderQueueSize;
		if ( tree.rootEntry() != null ) {
			this.buffer.load();
		}
		this.keyFunction = getKeyFunction;
		this.outOfOrderQueue = new PriorityBlockingQueue<>(outOfOrderQueueSize, new Comparator<TIN>() {

			@Override
			public int compare(final TIN o1, final TIN o2) {
				return (getKeyFunction.apply(o1).compareTo(getKeyFunction.apply(o2)));
			}
		});
		logger.info("Spare for appender: " + (spare * 100) + "%");
		logger.info("Events per leaf node: " + this.maxLeafEntries);
	}

	public FastAppender(final BPlusLink tree, final int bufferSize, final int idStepSize, final float spare, BinaryFunction<TID,TIN,TOUT> generateOut,
			final List<Pair<Long, ? extends Node>> treePath, final TIN first, final TIN last) {
		this(tree, bufferSize, idStepSize, spare, generateOut, treePath, first, last, 400);
	}

	public FastAppender(final BPlusLink tree, final int bufferSize, final int idStepSize, final float spare,
			BinaryFunction<TID,TIN,TOUT> generateOut,
			final List<Pair<Long, ? extends Node>> treePath, final TIN first, final TIN last,
			final int treeBufferSize) {
		this(tree, bufferSize, idStepSize, treeBufferSize, generateOut);
		this.maxIndexEntries = (int) (tree.getIndexNodeB() * (1.0f - spare));
		this.maxLeafEntries = (int) (tree.getLeafNodeB() * (1.0f - spare));

		this.first = first;
		this.last = last;

		this.outOfOrderQueueSize = 1;
		this.outOfOrderQueue = new PriorityBlockingQueue<>(this.outOfOrderQueueSize, new Comparator<TIN>() {

			@Override
			public int compare(final TIN o1, final TIN o2) {
				final Long k1 = (Long) tree.getKey.invoke(o1);
				final Long k2 = (Long) tree.getKey.invoke(o2);
				return Long.compare(k1, k2);
			}
		});
		this.wal = new NoLog<>();
		this.outOfOrderQueueDiskMirror = new NoLog<>();

		for (int i = 0; i < treePath.size(); i++) {
			this.buffer.treePath.add(i, new BufferEntry(treePath.get(i).getElement1(), treePath.get(i).getElement2()));
		}
		this.treeSize = treePath.size();

	}

	protected FastAppender(final BPlusLink tree, final int bufferSize, final int idStepSize, BinaryFunction<TID,TIN,TOUT> generateOut) {
		this(tree, bufferSize, idStepSize, 400, generateOut);
	}

	protected FastAppender(final BPlusLink tree, final int bufferSize, final int idStepSize, final int treeBufferSize, BinaryFunction<TID,TIN,TOUT> generateOut) {
		this.tree = tree;
		this.appenderContainer = new AppenderContainer(tree.container());
		this.buffer = new AppenderBuffer(this.appenderContainer, bufferSize, idStepSize);
		this.appenderContainer.setBuffer(this.buffer, new LRUBuffer<Object, Long, Object>(treeBufferSize));
		tree.setContainer(this.appenderContainer);
		this.bufferLock = new ReentrantLock(false);
		this.treeBufferSize = treeBufferSize;
		this.generateOut = generateOut;
	}

	public Container container() {
		return this.buffer.container;
	}

	public BPlusTree getTree() {
		return this.tree;
	}

	@Override
	public Future<TID> insertEntry(final TIN entry) {
		if ( this.first == null ) {
			this.first = entry;
		}
		this.last = entry;

		insert(entry, LEAF_LEVEL);
		long blockId = 0;
		int offset = 0;

		if ( this.buffer.treePath.size() > 0 ) {
			BufferEntry current = this.buffer.treePath.get(0);
			blockId = current.id;
			offset = current.node.entries.size()-1;
		}

		return new InsertFuture(new TID(blockId,offset));
	}

	@Override
	public Future<TID> outOfOrderInsert(final TIN entry) {

		this.outOfOrderQueue.offer(entry);

		this.outOfOrderQueueDiskMirror.insert(-1, entry);

		if ( this.outOfOrderQueue.size() >= this.outOfOrderQueueSize ) {
			pushOutOfOrderQueue();
		}

		return new InsertFuture();
	}

	private void pushOutOfOrderQueue() {
		try {
			this.bufferLock.lock();
			this.buffer.pushOut();
			if ( !this.outOfOrderQueue.isEmpty() ) {
				this.appenderContainer.outOfOrderUpdating = true;
				final IndexEntry rootEntry = createIndexEntry(this.buffer.root());
				this.tree.setRootEntry(rootEntry);
				this.tree.setRootDescriptor((Descriptor) rootEntry.separator().clone());
				final int s = this.outOfOrderQueue.size();
				for (int i = 0; i < s; i++) {
					outOfOrderTreeInsert(this.outOfOrderQueue.poll());
				}
				this.appenderContainer.outOfOrderUpdating = false;
			}
		} finally {
			this.bufferLock.unlock();
		}
		this.outOfOrderQueueDiskMirror.clear();
	}

	private void outOfOrderTreeInsert(final TIN entry) {
		final long ts = (Long) this.tree.key(entry);
		for (int i = 0; i < this.buffer.treePath.size() - 1; i++) {
			final Node node = this.buffer.get(i);
			final long entryTs = getFirstTimeStamp(node);
			if ( ts >= entryTs ) {
				insertIntoNode(node, entry);

				this.appenderContainer.outOfOrderUpdating = false;
				handleFullNode(node, i);
				this.appenderContainer.outOfOrderUpdating = true;
				return;
			}
		}

		final Node node = this.buffer.root();
		this.tree.insert(entry);
		this.appenderContainer.outOfOrderUpdating = false;
		handleFullNode(node, node.level);

		this.appenderContainer.outOfOrderUpdating = true;
		if ( getTimestamp(entry) < getTimestamp(this.first) ) {
			this.first = entry;

		}
	}

	private void handleFullNode(final Node node, final int level) {
		if ( treatFullNode(node, level) ) {
			this.buffer.pushOut();
			final IndexEntry rootEntry = createIndexEntry(this.buffer.root());
			this.tree.setRootEntry(rootEntry);
			this.tree.setRootDescriptor((Descriptor) rootEntry.separator().clone());
		}
	}

	protected boolean treatFullNode(final Node n, final int level) {
		if ( isFull(n, level) ) {
			if ( this.buffer.isFull() ) {
				this.buffer.pushOut();
			}

			this.buffer.handleFullNode(level);
			this.buffer.getForAdd(level);

			return true;
		}
		return false;
	}

	private void insertIntoNode(final Node node, final Object entry) {
		if ( node.level == LEAF_LEVEL ) {
			node.grow(entry);
		}
		else {
			final Comparable<?> c = this.tree.key(entry);
			final Descriptor descriptor = this.tree.createKeyRange(c, c);

			final Stack<MapEntry<?, ?>> path = new Stack<>();
			Tree.IndexEntry ie = createIndexEntry(node);
			do {
				ie = ie.chooseSubtree(descriptor, path);
			} while (ie.level() > LEAF_LEVEL);

			ie.growAndPost(entry, path);

			if ( ((Long) ie.id()).longValue() != this.buffer.treePath.get(0).id ) {

			}
		}
	}

	private IndexEntry createIndexEntry(final Node node) {
		final IndexEntry ie = this.tree.createIndexEntry(node.level + 1)
				.initialize(this.buffer.treePath.get(node.level).id);
		Separator sep;
		if ( node.number() > 0 ) {
			sep = (Separator) this.tree.separator(node.getLast()).clone();
		}
		else {
			sep = (Separator) this.tree.separator(node.previousNeighbor().get().getLast()).clone();
		}
		ie.initialize(sep);
		initializeIndexEntry(ie, node);
		return ie;
	}

	private long getFirstTimeStamp(final Node node) {

		final Node n = (Node) node;
		if ( n.previousNeighbor() == null )
			return Long.MAX_VALUE;
		else {
			final Node prevNode = (Node) n.previousNeighbor().get();
			if ( prevNode != null ) {

				if ( !this.appenderContainer.outOfOrderBuffer.contains(this.appenderContainer,
						(Long) n.previousNeighbor().id()) ) {
					this.appenderContainer.outOfOrderBuffer.update(this.appenderContainer, (Long) n.previousNeighbor().id(),
							prevNode, null, true);
				}
				if ( prevNode.level == LEAF_LEVEL )
					return (Long) this.tree.key(prevNode.getLast());
				else
					return (Long) ((IndexEntry) prevNode.getLast()).separator().sepValue();
			}
			else
				return Long.MAX_VALUE;
		}
	}

	private long getTimestamp(final Object event) {
		return ((Long) this.tree.key(event)).longValue();
	}

	@Override
	public void flushPathAndClose() {
		this.buffer.close();
		tree.setContainer(this.appenderContainer.container);
	}

	@Override
	public void flushPath() {
		if ( !this.buffer.treePath.isEmpty() ) {
			getTempRoot(true);
		}
		this.appenderContainer.flush();
	}

	public void emptyTreeBuffer() {
		appenderContainer.outOfOrderBuffer.flushAll(appenderContainer);
		appenderContainer.outOfOrderBuffer.removeAll(appenderContainer);
	}

	@Override
	public boolean hasData() {
		return true;
	}

	@Override
	public DoubleCursor<TOUT> query(final Long minKey, final Long maxKey) {
		if ( this.buffer.treePath.isEmpty() ||
			 this.keyFunction.apply(first).compareTo(maxKey) > 0 ||
			 this.keyFunction.apply(last).compareTo(minKey) < 0)
			return new EmptyDoubleCursor<>();
		getTempRoot(true);
		return this.tree.rangeQueryWithIds(generateOut, minKey, maxKey);
	}

	public DoubleCursor<TOUT> find(TID id) throws NoSuchEventException {
		return new LeafLevelCursor<>(container(), id, generateOut);
	}

	protected void initializeIndexEntry(final IndexEntry entry, final Node node) {
	}

	protected void updateNode(final Node n, final Object entry) {
		n.entries.add(entry);
	}

	protected void insert(final Object entry, final int level) {

		if ( level >= this.treeSize ) {
			resizeTree(level);
		}

		Node n = this.buffer.getForAdd(level);
		updateNode(n, entry);

		if ( isFull(n, level) )
			this.buffer.handleFullNode(level);
	}

	private void resizeTree(final int level) {

		final Node node = (Node) this.tree.createNode(level);
		this.buffer.add(node, level);
		this.treeSize++;
	}

	@Override
	public Pair<IndexEntry, Node> getTempRoot(final boolean updateAggregates) {
		if ( updateAggregates ) {
			pushOutOfOrderQueue();
		}
		final Pair<IndexEntry, Node> e = this.buffer.copyRoot();
		this.tree.setRootEntry(e.getElement1());
		this.tree.setRootDescriptor(this.tree.createKeyRange(this.tree.key(this.first), this.tree.key(this.last)));
		return e;
	}

	protected boolean isFull(final Node n, final int level) {
		return n.entries.size() >= ((level == LEAF_LEVEL) ? this.maxLeafEntries : this.maxIndexEntries);
	}

	private boolean isNearlyFull(final Node n, final int level) {
		return n.entries.size() + 1 == ((level == LEAF_LEVEL) ? this.maxLeafEntries : this.maxIndexEntries);
	}

	public TIN getFirst() {
		return this.first;
	}

	public TIN getLast() {
		return this.last;
	}

	private class AppenderContainer extends AbstractContainer {

		private final Container					container;

		AppenderBuffer							buffer;

		protected Buffer<Object, Long, Object>	outOfOrderBuffer;

		protected boolean						outOfOrderUpdating;

		public AppenderContainer(final Container container) {
			this.container = container;
		}

		public void setBuffer(final AppenderBuffer buffer, final Buffer<Object, Long, Object> outOfOrderBuffer) {
			this.buffer = buffer;
			this.outOfOrderBuffer = outOfOrderBuffer;
		}

		@Override
		public void close() {
			super.close();
			this.outOfOrderBuffer.flushAll(this);
			this.container.close();
		}

		@Override
		public synchronized Object get(final Object id, final boolean unfix) throws NoSuchElementException {
			try {
				FastAppender.this.bufferLock.lock();
				final long idVal = (Long) id;

				if ( this.buffer.containsEntry(idVal) ) {
					for ( BufferEntry entry : this.buffer.treePath ) {
						if ( entry.id == idVal ) {
							if ( this.outOfOrderUpdating )
								return entry.node;
							else
								return copyNode(entry.node, true);
						}
					}
					for ( BufferWriteEntry entry : this.buffer.nodeBuffer ) {
						if ( entry.id == idVal ) {
							return entry.node;
						}
					}
				}
				final Object result = this.outOfOrderBuffer.get(this, idVal, new AbstractFunction<Long, Object>() {

					private static final long serialVersionUID = 1L;

					@Override
					public Object invoke(final Long argument) {
						return AppenderContainer.this.container.get(argument, true);
					}
				}, true);
				return result;
			} finally {
				FastAppender.this.bufferLock.unlock();
			}
		}

		@Override
		public Iterator ids() {
			final SortedSet<Long> inMemIds = new TreeSet<>();
			for ( BufferEntry be : buffer.treePath ) {
				inMemIds.add( be.id );
			}
			for ( BufferWriteEntry bwe : buffer.nodeBuffer ) {
				inMemIds.add( bwe.id );
			}

			return new Iterator () {
				final Iterator containerSource = container.ids();
				Iterator remaining = null;

				@Override
				public boolean hasNext() {

					return containerSource.hasNext() || (remaining != null && remaining.hasNext());
				}

				@Override
				public Object next() {
					Object result = null;
					if ( containerSource.hasNext() ) {
						result = containerSource.next();
						inMemIds.remove(result);

						if ( !containerSource.hasNext() ) {
							remaining = inMemIds.iterator();
						}
					}
					else if ( remaining.hasNext() ) {
						result = remaining.next();
					}
					else {
						throw new NoSuchElementException("Cannot go beyond last element.");
					}
					return result;
				}

			};
		}

		@Override
		public boolean isUsed(final Object id) {
			final long idVal = (Long) id;
			if ( this.buffer.containsEntry(idVal) )
				return true;
			else
				return this.container.isUsed(id);
		}

		@Override
		public FixedSizeConverter<?> objectIdConverter() {
			return this.container.objectIdConverter();
		}

		@Override
		public void remove(final Object id) throws NoSuchElementException {
			this.container.remove(id);
		}

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Object reserve(final Function getObject) {
			Object oj = this.container.reserve(getObject);

			while (getObject != null && this.buffer.containsEntry((Long) oj)) {
				oj = this.container.reserve(getObject);
			}
			return oj;
		}

		@Override
		public int size() {
			return this.container.size();
		}

		@Override
		public void update(final Object id, final Object object, final boolean unfix) throws NoSuchElementException {

			try {
				FastAppender.this.bufferLock.lock();
				if ( !unfix && this.buffer.containsEntry((Long) id) ) {
					this.buffer.replace((Long) id, (Node) object);
				}
				else {
					final long lid = (Long) id;
					if ( lid > this.buffer.treePath.get(0).id ) {
						if ( ((Node) object).level >= this.buffer.treePath.size() ) {
							logger.debug("New root node");
							this.buffer.treePath.add(new BufferEntry((Long) id, (Node) object));
							return;
						}
						this.container.update(id, object, unfix);
					}
					if ( this.outOfOrderUpdating ) {
						this.outOfOrderBuffer.update(this, lid, object, new AbstractFunction<Object, Object>() {

							private static final long serialVersionUID = 1L;

							@Override
							public Object invoke(final Object argument0, final Object argument1) {
								AppenderContainer.this.container.update(argument0, argument1, unfix);
								return null;
							}
						}, true);
						return;
					}
					else {
						this.outOfOrderBuffer.update(this, lid, object, null, true);
						if ( lid <= this.buffer.treePath.get(0).id ) {
							this.container.update(id, object, unfix);
						}
					}
				}
			} finally {
				FastAppender.this.bufferLock.unlock();
			}
		}

		@Override
		public void flush() {
			super.flush();
			this.outOfOrderBuffer.flushAll(this);
			this.container.flush();
		}

		@Override
		public int hashCode() {
			return 0;
		}
	}

	protected Node copyNode(final Node node, final boolean updateAggregates) {
		final Node copy = (Node) this.tree.createNode(node.level);
		copy.setNextNeighbor(node.nextNeighbor());
		copy.setPreviousNeighbor(node.previousNeighbor());
		copy.entries.addAll(node.entries);
		return copy;
	}

	protected void updateReferences(final BufferEntry oldPair, final BufferEntry newPair, final int level) {
		oldPair.node.setNextNeighbor(this.tree.createIndexEntry(level).initialize(newPair.id));
		newPair.node.setPreviousNeighbor(this.tree.createIndexEntry(level).initialize(oldPair.id));
	}

	private void informListeners(final BufferEntry entry) {
		if ( entry.node.level > LEAF_LEVEL )
			return;

		for (int i = 0; i < entry.node.number(); i++) {

			new TID(entry.id, i);
		}
	}

	protected class AppenderBuffer {

		private final int				idStepSize;

		private final int				bufferSize;

		private final List<BufferWriteEntry>	nodeBuffer;

		protected List<BufferEntry>		treePath;

		protected AppenderContainer		container;

		public AppenderBuffer(final AppenderContainer container, final int bufferSize, final int idStepSize) {
			this.container = container;
			this.bufferSize = bufferSize;
			this.idStepSize = idStepSize;
			this.nodeBuffer = new ArrayList<>(bufferSize);
			this.treePath = new ArrayList<>();
		}

		public Node get(final int level) {
			return this.treePath.get(level).node;
		}

		public Node root() {
			return this.treePath.get(this.treePath.size() - 1).node;
		}

		public void add(final Node n, final int level) {

			final long nextLevelID = firstLevelID(level);
			this.treePath.add(level, new BufferEntry(nextLevelID, n));
		}

		public boolean containsEntry(final long id) {
			for (final BufferEntry be : this.treePath)
				if ( be.id == id )
					return true;
			for (final BufferWriteEntry be : this.nodeBuffer)
				if ( be.id == id )
					return true;
			return false;
		}

		public void replace(final long id, final Node node) {
			for (final BufferEntry be : this.treePath) {
				if ( be.id == id ) {
					if ( node.level() != be.node.level() )
						throw new RuntimeException("Wrong node [" + id + "] at level " + node.level());
					be.node = copyNode(node,false);
					break;
				}
			}
			for (final BufferWriteEntry be : this.nodeBuffer) {
				if ( be.id == id ) {
					if ( node.level() != be.node.level() )
						throw new RuntimeException("Wrong node [" + id + "] at level " + node.level());
					be.node = copyNode(node, false);
					break;
				}
			}
		}

		public boolean isFull() {
			return this.nodeBuffer.size() >= this.bufferSize;
		}

		public void pushOut() {
			try {
				FastAppender.this.bufferLock.lock();
				for ( BufferWriteEntry entry : this.nodeBuffer ) {

					if ( entry.nextId != null ) {
						IndexEntry ie = tree.createIndexEntry(entry.node.level+1).initialize(entry.nextId);
						entry.node.setNextNeighbor(ie);
					}
					this.container.update(entry.id, entry.node, true);
					informListeners(entry);
				}
				this.nodeBuffer.clear();
			} finally {
				FastAppender.this.bufferLock.unlock();
			}
		}

		private int getAdditionalOffset(final int level) {

			if ( level < this.treePath.size() && isNearlyFull(this.treePath.get(level).node, level) )
				return this.idStepSize + getAdditionalOffset(level + 1);
			else
				return 0;
		}

		public Node getForAdd(final int level) {
			final BufferEntry be = this.treePath.get(level);
			if ( FastAppender.this.isFull(be.node,level) ) {
				final Node newNode = FastAppender.this.tree.createNode(level);

				if ( be.nextId == null )
					throw new IllegalStateException("Full nodes must have a set nextId (predicted next neighbor)");

				final BufferEntry newPair = new BufferEntry(be.nextId, newNode);

				updateReferences(be, newPair, level);

				try {
					FastAppender.this.bufferLock.lock();
					this.treePath.set(level, newPair);
				} finally {
					FastAppender.this.bufferLock.unlock();
				}
				return newNode;
			}
			else {
				return be.node;
			}
		}

		public void handleFullNode(final int level) {
			final BufferEntry be = this.treePath.get(level);
			final long        realId   = (Long) this.container.reserve(null);

			if ( realId != be.id ) {
				logger.warn("Repairing ID. Level=" + level + ", predicted=" + be.id + ", actual=" + realId);
				System.out.println("Repairing ID. Level=" + level + ", predicted=" + be.id + ", actual=" + realId);
				repairReferences((Node) be.node, be.id, realId);
				be.id = realId;
			}

			final Separator sep = (Separator) FastAppender.this.tree.separator(be.node.getLast()).clone();
			final IndexEntry entry = FastAppender.this.tree.createIndexEntry(level + 1).initialize(be.id,sep);

			initializeIndexEntry(entry, be.node);

			try {
				FastAppender.this.bufferLock.lock();

				be.nextId = nextLevelID(be.id, level);
				this.nodeBuffer.add(new BufferWriteEntry(be.id, copyNode(be.node,false), be.nextId));
			} finally {
				FastAppender.this.bufferLock.unlock();
			}

			if ( isFull() ) {
				pushOut();
			}

			insert(entry, level+1);
		}

		private void repairReferences(final Node n, final Comparable id, final long newID) {
			IndexEntry ie = n.previousNeighbor();
			if ( ie == null )
				return;

			Node prev = (Node) ie.get();

			while (!prev.nextNeighbor().id().equals(id)) {
				ie = prev.nextNeighbor();
				prev = (Node) ie.get();
			}
			n.previousNeighbor().initialize(ie.id());
			prev.nextNeighbor().initialize(newID);
			ie.update(prev);
		}

		private Optional<BufferWriteEntry> getFromNodeBuffer( long id ) {
			for ( BufferWriteEntry be : nodeBuffer ) {
				if ( be.id == id )
					return Optional.of(be);
			}
			return Optional.empty();
		}

		protected long nextLevelID(final long id, final int level) {
			return id + idsBetween(level) * this.idStepSize + this.idStepSize + getAdditionalOffset(level + 1);
		}

		protected long firstLevelID(final int level) {
			return idsBetween(level) * this.idStepSize;
		}

		private long idsBetween(final int level) {
			long output = 0;
			for (int i = 1; i <= level; i++) {
				output += Math.pow(FastAppender.this.maxIndexEntries, i);
			}
			return output;
		}

		protected IndexEntry createIndexEntry(final Node node, final long id) {
			final Separator sep = (Separator) FastAppender.this.tree.separator(node.getLast()).clone();
			final IndexEntry newIndexEntry = FastAppender.this.tree.createIndexEntry(node.level + 1).initialize(id,sep);

			initializeIndexEntry(newIndexEntry, node);
			return newIndexEntry;
		}

		protected Pair<IndexEntry, Node> copyRoot() {

			IndexEntry child = null;
			Node node = null;
			if ( this.treePath.get(0).node.number() > 0 ) {
				node = this.treePath.get(0).node;
				child = createTmpIndexEntry(node, this.treePath.get(0).id);

			}
			for (int i = 1; i < this.treePath.size(); i++) {
				final BufferEntry e = this.treePath.get(i);
				node = copyNode(e.node, true);
				if ( child != null && (node.number() == 0 || !((IndexEntry)node.getLast()).id().equals(child.id())) ) {
					node.entries.add(child);
				}
				if ( node.number() > 0 ) {
					child = createTmpIndexEntry(node, e.id);

				}
				else {
					child = null;
				}
			}
			return new Pair<>(child, node);
		}

		protected IndexEntry createTmpIndexEntry(final Node node, final Object id) {
			final Separator sep = (Separator) FastAppender.this.tree.separator(node.getLast()).clone();
			final IndexEntry newIndexEntry = FastAppender.this.tree.createTempIndexEntry(node);
			newIndexEntry.initialize(sep).initialize(id);

			initializeIndexEntry(newIndexEntry, node);
			return newIndexEntry;
		}

		protected void close() {

			if ( FastAppender.this.last == null ) {
				FastAppender.this.tree.setRootEntry(null);
				FastAppender.this.tree.setRootDescriptor(null);
				logger.debug("Closed FastAppender (no-data).");
				return;
			}

			logger.debug("Closing FastAppender.");

			logger.debug("Pushing out-of-order queue.");
			pushOutOfOrderQueue();

			final Separator sep = (Separator) FastAppender.this.tree.separator(FastAppender.this.last).clone();

			logger.debug("Persisting right tree flank.");

			for ( int i = 0; i < this.treePath.size(); i++ ) {
				BufferEntry be = this.treePath.get(i);
				final Node node = be.node;
				long id = be.id;

				if ( be.nextId != null ) {
					logger.debug("Level " + node.level + " page was already persisted.");
				}
				else {
					id = (Long) this.container.reserve(null);
					final IndexEntry nn = FastAppender.this.tree.createIndexEntry(node.level + 1).initialize(-be.id);
					logger.debug("Level " + node.level + " is persisted to " + id + ", predicted id (" + be.id + ") is written to nextNeighbor reference.");
					node.setNextNeighbor(nn);

					if ( node.level() < treePath.size()-1 ) {
						final IndexEntry parentIndexEntry = FastAppender.this.tree.createIndexEntry(node.level + 1).initialize(id,sep);
						initializeIndexEntry(parentIndexEntry, node);
						insert(parentIndexEntry, node.level() + 1);
					}
					this.container.container.update(id, node, true);
				}

				if ( node.level() == treePath.size()-1 ) {
					final IndexEntry parentIndexEntry = FastAppender.this.tree.createIndexEntry(node.level + 1).initialize(id,sep);
					FastAppender.this.tree.setRootEntry(parentIndexEntry);
					FastAppender.this.tree.setRootDescriptor(FastAppender.this.tree.createKeyRange(
							FastAppender.this.tree.key(FastAppender.this.first),
							FastAppender.this.tree.key(FastAppender.this.last)));
				}
			}

			pushOut();
			logger.debug("Flushing random access buffer.");
			FastAppender.this.appenderContainer.flush();
			logger.debug("Finished closing FastAppender.");
		}

		protected void load() {

			IndexEntry ie = FastAppender.this.tree.rootEntry();
			FastAppender.this.treeSize = ie.level() + 1;
			Node node = (Node) ie.get();
			long realID;
			long id = (Long) ie.id();

			List<Long> idsToDelete = new ArrayList<>();

			if ( node.level > LEAF_LEVEL ) {
				do {

					if ( node.nextNeighbor() != null && ((Long)node.nextNeighbor().id()) <= 0L ) {
						realID = -((Long) node.nextNeighbor().id());
						node.setNextNeighbor(null);
						idsToDelete.add(id);
						treePath.add(0,new BufferEntry(realID, node));
					}

					else if ( node.nextNeighbor() != null ) {
						realID = id;
						long nextId = (Long) node.nextNeighbor().id();
						node.setNextNeighbor(null);
						treePath.add(0,new BufferEntry(realID, node, nextId));
					}

					else {
						throw new IllegalStateException("Right flank must posses a nextNeighbour with negative id value.");
					}

					if ( node.level > LEAF_LEVEL ) {
						ie = (IndexEntry) node.getLast();
						id = (Long) ie.id();

						Node childNode = (Node) ie.get();

						if ( !FastAppender.this.isFull(childNode, childNode.level) )
							node.entries.remove(node.entries.size() - 1);

						node = childNode;
					}

				} while (node.level > LEAF_LEVEL);
			}

			if ( node.nextNeighbor() != null && ((Long)node.nextNeighbor().id()) <= 0 ) {
				realID = -((Long) node.nextNeighbor().id());
				node.setNextNeighbor(null);
				idsToDelete.add(id);
				treePath.add(0,new BufferEntry(realID, node));
			}

			else if ( node.nextNeighbor() != null ) {
				realID = id;
				long nextId = (Long) node.nextNeighbor().id();
				node.setNextNeighbor(null);
				treePath.add(0,new BufferEntry(realID, node, nextId));
			}
			else {
				throw new IllegalStateException("Right flank must posses a nextNeighbour with negative id value.");
			}

			logger.debug("Deleting flank pages: " + idsToDelete);
			Collections.sort(idsToDelete, Comparator.comparingLong(x -> (Long)x).reversed() );
			idsToDelete.forEach(x -> this.container.remove(x));

			FastAppender.this.tree.rootEntry().initialize(this.treePath.get(this.treePath.size() - 1).id);

			Node lastLeaf = treePath.get(LEAF_LEVEL).node;

			while (lastLeaf.number() < 1 && lastLeaf.previousNeighbor() != null ) {
				lastLeaf = lastLeaf.previousNeighbor().get();
			}
			if ( lastLeaf.number() > 0 ) {
				FastAppender.this.last = (TIN) lastLeaf.getLast();
			}
			else {
				return;
			}

			if ( FastAppender.this.last != null ) {
				Node n = null;
				for (int level = treePath.size()-1; n == null && level >= 0; level-- ) {
					n = treePath.get(level).node;
					if ( n.number() == 0 )
						n = null;
				}
				while ( n.level() > LEAF_LEVEL ) {
					n = ((IndexEntry) n.getFirst()).get();
				}
				FastAppender.this.first = (TIN) n.getFirst();
			}
			else {
				logger.error("No entry found while loading appender state!");
			}
		}
	}

	@Override
	public int getMaxLeafEntries() {
		return this.maxLeafEntries;
	}

	@Override
	public int getMaxIndexEntries() {
		return this.maxIndexEntries;
	}

}
