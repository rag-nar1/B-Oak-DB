package bufferpool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Frame {

    private int frameId;
    private long pageId;
    private String fileName;
    private boolean dirty;
    private byte[] data;
    private AtomicInteger pinCount;
    private ReadWriteLock latch;

    public Frame(){}
    public Frame(int frameId){
        this.frameId = frameId;
        this.data = new byte[4096];
        pinCount = new AtomicInteger();
        latch = new ReentrantReadWriteLock();
    }
    public Frame(int frameId, int pageId, String fileName) {
        this.frameId = frameId;
        this.pageId = pageId;
        this.fileName = fileName;
        this.data = new byte[4096];
        pinCount = new AtomicInteger();
        latch = new ReentrantReadWriteLock();
    }

    public void newFrame(long pageId, String fileName) {
        dirty = false;
        this.pageId = pageId;
        this.fileName = fileName;
        pinCount = new AtomicInteger();
    }

    public String getFileName() {
        return fileName;
    }

    public long getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getFrameId() {
        return frameId;
    }

    public int addPin() {
        return pinCount.incrementAndGet();
    }
    
    public int removePin() {
        return pinCount.decrementAndGet();
    }
    
    public int getPinCount() {
        return pinCount.get();
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public void lockRead() {
        latch.readLock().lock();
    }

    public void lockWrite() {
        latch.writeLock().lock();
    }

    public void unlockRead() {
        latch.readLock().unlock();
    }

    public void unlockWrite() {
        latch.writeLock().unlock();
    }
}
