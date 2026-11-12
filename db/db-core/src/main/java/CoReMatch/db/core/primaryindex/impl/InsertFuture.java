package CoReMatch.db.core.primaryindex.impl;

import CoReMatch.db.event.TID;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InsertFuture implements Future<TID> {

    /**
     * The lock blocking threads on {@link #get()} until a result is
     * {@link #setResult(TID) set}
     */
    private final ReadWriteLock lock;
    /** The result */
    private TID result;

    /**
     * Creates a new InsertFuture instance, wainting for a result
     */
    public InsertFuture() {
        this.result = null;
        this.lock = new ReentrantReadWriteLock();
        this.lock.writeLock().lock();
    }

    /**
     * Creates a new InsertFuture instance with an already computed result
     *
     * @param result
     *            this Future's result
     */
    public InsertFuture(final TID result) {
        this.result = result;
        this.lock = null;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return this.result != null;
    }

    @Override
    public TID get() throws InterruptedException, ExecutionException {
        if (result != null)
            return result;

        try {
            this.lock.readLock().lock();
            return this.result;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void setResult(final TID result) {
        this.result = result;
        this.lock.writeLock().unlock();
    }

    @Override
    public TID get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (result != null)
            return result;
        try {
            this.lock.readLock().tryLock(timeout, unit);
            return this.result;
        } finally {
            this.lock.readLock().unlock();
        }
    }

}
