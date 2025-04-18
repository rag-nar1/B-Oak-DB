package bufferpool;

import java.io.IOException;
import java.lang.reflect.Array;
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

import diskmanager.DiskManeger;
import diskmanager.DiskRequest;

public class BufferPool {

    public class PageId extends SimpleEntry<String, Long> {
        public PageId(String key, Long value) {
            super(key, value);
        }
    }

    private int framesNumber;
    private Replacer replacer;
    private DiskManeger diskManeger;
    private Frame[] frames;
    private Map<PageId, Integer> pages;
    private List<Integer> freeFrames;
    private Map<String, SortedSet<Long>> deallocatedPages;
    private Lock bpmLatch;

    public BufferPool(int size, Replacer replacer, DiskManeger diskManeger) {
        framesNumber = size;
        this.replacer = replacer;
        this.diskManeger = diskManeger;
        frames = new Frame[size];
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
        return diskManeger.allocatePage(fileName);
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
        diskManeger.pushRequest(request);
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
        return frameId;
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
        PageId pid = new PageId(fileName, pageId);
        int frameId;
        bpmLatch.lock();
        if (pages.containsKey(pid)) { // if the page already in the buffer 
            frameId = pages.get(pid);
            Frame frame = frames[frameId];
            bpmLatch.unlock();
            Guard guard;
            if (isWrite) {
                guard = new WriteGuard(frameId, frame, replacer, bpmLatch);
            } else {
                guard = new ReadGuard(frameId, frame, replacer, bpmLatch);
            }
            bpmLatch.unlock();
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
            return null;
        }

        pages.put(pid, frameId);
        bpmLatch.unlock();
        Guard guard;
        if (isWrite) {
            guard = new WriteGuard(frameId, frame, replacer, bpmLatch);
        } else {
            guard = new ReadGuard(frameId, frame, replacer, bpmLatch);
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
    public WriteGuard getWriteGuard(String fileName, long pageId) throws Exception, InterruptedException, NullPointerException, ExecutionException {
        Guard guard = getGuard(fileName, pageId, false);
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
        
        SortedSet<Long> fileFreePages = deallocatedPages.get(fileName);
        if (fileFreePages == null) {
            fileFreePages = new TreeSet<Long>();
        }
        
        fileFreePages.add(pageId);
    }

    


}
