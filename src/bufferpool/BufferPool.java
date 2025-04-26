package bufferpool;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.AbstractMap.SimpleEntry;

import diskmanager.DiskManager;
import diskmanager.DiskRequest;
import globals.Globals;

public class BufferPool implements Closeable {

    public class PageId extends SimpleEntry<String, Long> {
        public PageId(String key, Long value) {
            super(key, value);
        }
    }

    private int framesNumber;
    private int k;
    private Replacer replacer;
    private DiskManager diskManager;
    private Frame[] frames;
    private Map<PageId, Integer> pages;
    private List<Integer> freeFrames;
    private Map<String, SortedSet<Long>> deallocatedPages;
    private Lock bpmLatch;

    public BufferPool(int size, int k, DiskManager diskManager) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be positive");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (diskManager == null) {
            throw new NullPointerException("disk manager can not be null");
        }
        framesNumber = size;
        this.k = k;
        this.replacer = new Replacer(this.k);
        this.diskManager = diskManager;
        frames = new Frame[framesNumber];
        freeFrames = new LinkedList<Integer>();
        pages = new HashMap<PageId, Integer>();
        deallocatedPages = new HashMap<String, SortedSet<Long>>();
        bpmLatch = new ReentrantLock();

        for(int i = 0; i < size; i ++) {
            freeFrames.addLast(i);
            frames[i] = new Frame(i);
        }
    }

    /**
     * close the buffer pool and all the frames
     * @throws IOException
     */
    public void close() throws IOException, NullPointerException {
        // flush all the frames
        for (int i = 0; i < framesNumber; i++) {
            Frame frame = frames[i];
            if (frame.isDirty()) {
                boolean done;
                try {
                    done = diskOp(frame, true);
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("Error during disk operation", e);
                }
                if (!done) {
                    throw new IOException("can not flush");
                }
            }
        }
        // close the disk manager
        diskManager.close();
        // invalidate the buffer pool
        for (int i = 0; i < framesNumber; i++) {
            frames[i] = null;
        }
        frames = null;
        pages = null;
        freeFrames = null;
        deallocatedPages = null;
        bpmLatch = null;
        replacer = null;
        framesNumber = -1;
        diskManager = null;
    }

    /**
     * allocate a new page in the passed file and return the pageid
     * if the file has deallocated pages it returns one of them to prrevent Fragmentation
     * @param fileName
     * @return pageId of allocated page
     * @throws IOException
     * @throws NullPointerException
     */
    public long allocateNewPage(String fileName) throws IOException, NullPointerException {
        bpmLatch.lock();
        if (deallocatedPages.containsKey(fileName)) {
            SortedSet<Long> pages = deallocatedPages.get(fileName);
            if (!pages.isEmpty()) {
                long pageId = pages.getLast();
                pages.removeLast();
                bpmLatch.unlock();
                return pageId;
            }
        }
        bpmLatch.unlock();
        return diskManager.allocatePage(fileName);
    }

    /**
     * create a disk request (read/write) push it to the queue and wait for complation
     * @param frame frame in which will be read into/wrote to 
     * @param isWrite tells if the request is read or write
     * @return true on success
     * @throws InterruptedException
     * @throws NullPointerException
     * @throws ExecutionException
     */
    private boolean diskOp(Frame frame, boolean isWrite) throws InterruptedException, NullPointerException, ExecutionException {
        DiskRequest request = new DiskRequest(frame.getFileName(), frame.getPageId(),frame.getData(), isWrite);
        CompletableFuture<Boolean> finish = request.getFuture();
        diskManager.pushRequest(request);
        return finish.get();
    }

    /**
     * return a free frame from the free frame list or by evicting some unused frame
     * @return the frame id
     * @throws Exception
     * @throws InterruptedException
     * @throws NullPointerException
     * @throws ExecutionException
     */
    private int getFrame() throws Exception, InterruptedException, NullPointerException, ExecutionException {
        int frameId;
        // if a free frame already exists
        if (!freeFrames.isEmpty()) {
            frameId = freeFrames.getLast();
            freeFrames.removeLast();
            return frameId;
        }

        // try to evict
        frameId = replacer.evict();
        if (frameId == -1) {
            return frameId;
        }
        Frame frame = frames[frameId];
        if (frame.isDirty()) {
            boolean done = diskOp(frame, true);
            if (!done) {
                throw new Exception("can not flush");
            }
        }
        // remove the page from the pages map
        PageId pid = new PageId(frame.getFileName(), frame.getPageId());
        if (pages.containsKey(pid)) {
            pages.remove(pid);
        }
        return frameId;
    }

    private void recordAccess(Frame frame) {
        replacer.recordAccess(frame.getFrameId());
        frame.addPin();
        if (frame.getPinCount() == 1) {
            replacer.setEvictable(frame.getFrameId(), false);
        }
    }

    private boolean isPageIdValid(String fileName, long pageId) {
        if (deallocatedPages.containsKey(fileName)) {
            SortedSet<Long> pages = deallocatedPages.get(fileName);
            if (pages.contains(pageId)) {
                return false;
            }
        }
        if (diskManager.getPageCount(fileName) <= pageId) {
            return false;
        }
        return true;
    }

    /**
     * returns a guard(read/write) around a specific page after reading it to a free frame
     * @param fileName
     * @param pageId
     * @param isWrite
     * @return
     * @throws Exception
     * @throws InterruptedException
     * @throws NullPointerException
     * @throws ExecutionException
     */
    private Guard getGuard(String fileName, long pageId, boolean isWrite) throws Exception, InterruptedException, NullPointerException, ExecutionException {
        bpmLatch.lock();
        if (pageId < 0) {
            bpmLatch.unlock();
            throw new IllegalArgumentException("pageId must be positive");
        }

        if (!isPageIdValid(fileName, pageId)) {
            bpmLatch.unlock();
            throw new IllegalArgumentException("pageId is not valid");
        }
        
        if (fileName == null) {
            bpmLatch.unlock();
            throw new NullPointerException("fileName can not be null");
        }

        if (fileName.isEmpty()) {
            bpmLatch.unlock();
            throw new IllegalArgumentException("fileName can not be empty");
        }

        PageId pid = new PageId(fileName, pageId);
        int frameId;
        if (pages.containsKey(pid)) { // if the page already in the buffer 
            frameId = pages.get(pid);
            Frame frame = frames[frameId];
            recordAccess(frame);
            bpmLatch.unlock();
            Guard guard;
            if (isWrite) {
                guard = new WriteGuard(frameId, frame, replacer, bpmLatch);
            } else {
                guard = new ReadGuard(frameId, frame, replacer, bpmLatch);
            }

            if (guard.getFrameId() == Globals.INVALID_Frame_ID) {
                return null;
            }
            return guard;
        }

        frameId = getFrame();
        if (frameId == -1) {
            bpmLatch.unlock();
            return null;
        } 

        Frame frame = frames[frameId];
        frame.newFrame(pageId, fileName);
        if (!diskOp(frame, false)) {
            bpmLatch.unlock();
            return null;
        }

        pages.put(pid, frameId);
        recordAccess(frame);
        bpmLatch.unlock();
        Guard guard;
        if (isWrite) {
            guard = new WriteGuard(frameId, frame, replacer, bpmLatch);
        } else {
            guard = new ReadGuard(frameId, frame, replacer, bpmLatch);
        }
        if (guard.getFrameId() == Globals.INVALID_Frame_ID) {
            return null;
        }
        return guard;
    }

    /**
     * return a read guard 
     * @param fileName
     * @param pageId
     * @return
     * @throws Exception
     * @throws InterruptedException
     * @throws NullPointerException
     * @throws ExecutionException
     */
    public ReadGuard getReadGuard(String fileName, long pageId) throws Exception, InterruptedException, NullPointerException, ExecutionException {
        Guard guard = getGuard(fileName, pageId, false);
        if (guard == null) {
           return null;
        }
        ReadGuard readGuard = (ReadGuard) guard;
        return readGuard;
    }

    /**
     * returns a write guard
     * @param fileName
     * @param pageId
     * @return
     * @throws Exception
     * @throws InterruptedException
     * @throws NullPointerException
     * @throws ExecutionException
     */
    public WriteGuard getWriteGuard(String fileName, long pageId) throws Exception, InterruptedException, NullPointerException, ExecutionException, IllegalArgumentException {
        Guard guard = getGuard(fileName, pageId, true);
        if(guard == null) {
           return null;
        }
        WriteGuard writeGuard = (WriteGuard) guard;
        return writeGuard;
    }

    public void deletePage(String fileName, long pageId) {
        // if page exists in the pool
        PageId pid = new PageId(fileName, pageId);
        bpmLatch.lock();
        if (pages.containsKey(pid)) {
            int frameId = pages.get(pid);
            pages.remove(pid);
            replacer.deleteFrame(frameId);
        }

        if (!deallocatedPages.containsKey(fileName)) {
            deallocatedPages.put(fileName, new TreeSet<Long>());
        }
        
        SortedSet<Long> fileFreePages = deallocatedPages.get(fileName);
        if (fileFreePages == null) {
            fileFreePages = new TreeSet<Long>();
        }
        
        fileFreePages.add(pageId);
    }

}
