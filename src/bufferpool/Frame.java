package bufferpool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Frame {

    private int frameId;
    private int pageId;
    private boolean dirty;
    private byte[] data;
    private AtomicInteger pinCount;
    private ReadWriteLock latch;
    private Lock lock;

    public Frame(int frameId, int pageId, byte[] data) {
        this.frameId = frameId;
        this.pageId = frameId;
        this.data = data;
        pinCount = new AtomicInteger();
        latch = new ReentrantReadWriteLock();
    }

    public void lockRead() {
       lock = latch.readLock();
       lock.lock();
    }

    public void lockWrite() {
        lock = latch.writeLock();
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
