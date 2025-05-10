package diskmanager;

import java.io.IOException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import globals.Globals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class BasicDiskManager implements DiskManager {

    private Map<String, DiskFile> files;
    private Map<String, List<Long>> filesFreePages;
    private Map<String, Lock> resizeLocks;
    private int fileCount;
    private BlockingQueue<DiskRequest> requestQueue;
    private Thread mainThread;

    private final String storageDir = "storage/";

    /**
     * defualt constructre
     * todo: make new constructors based on a config
     */
    public BasicDiskManager() throws NullPointerException {
        this.files = new ConcurrentHashMap<String, DiskFile>();
        this.filesFreePages = new ConcurrentHashMap<String, List<Long>>();
        this.resizeLocks = new ConcurrentHashMap<String, Lock>();
        this.fileCount = 0;
        this.requestQueue = new LinkedBlockingQueue<DiskRequest>(100); // capped to 100 requests
        mainThread = new Thread(() -> { // start the worker that would fetch the requests and start a thread for each
            try {
                run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                e.printStackTrace(); // Log the exception
            }
        });
        mainThread.start();
    }

    /**
     * closes all files and invalidate the requestQueue and the files map
     * used as a destractor
     * 
     * @throws NullPointerException if the object used after calling the close
     *                              method for the first time
     */
    public void close() throws NullPointerException {
        mainThread.interrupt();
        for (DiskRequest diskRequest : requestQueue) { // notify all that requests are not done
            diskRequest.finish.complete(false);
        }
        requestQueue = null;
        files.forEach((key, value) -> {
            try {
                value.close();
            } catch (IOException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                e.printStackTrace(); // Log the exception
            }
        });
        files = null;
        fileCount = -1;
        mainThread = null;
    }

    /**
     * the main threads run here start by fetching a request from the queue spin a
     * thread to hundle it
     * 
     * @throws InterruptedException while ferching a request
     * @throws NullPointerException if used after a close call
     */
    private void run() throws InterruptedException, NullPointerException {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DiskRequest currentRequest = requestQueue.take();
                new Thread(() -> {
                    DiskFile file = this.files.get(currentRequest.fileName);
                    if (file == null) {
                        try {
                            open(currentRequest.fileName);
                            file = this.files.get(currentRequest.fileName);
                        } catch (Exception e) {
                            Thread.currentThread().interrupt(); // Restore interrupted status
                            e.printStackTrace(); // Log the exception
                            return;
                        }
                    }

                    if (currentRequest.isWrite) {
                        try {
                            file.writePage(currentRequest.pageID, currentRequest.data);
                            currentRequest.finish.complete(true);
                        } catch (IOException e) {
                            e.printStackTrace(); // Log the exception
                            Thread.currentThread().interrupt(); // Restore interrupted status
                        }
                    } else {
                        try {
                            file.readPage(currentRequest.pageID, currentRequest.data);
                            currentRequest.finish.complete(true);
                        } catch (IOException e) {
                            e.printStackTrace(); // Log the exception
                            Thread.currentThread().interrupt(); // Restore interrupted status
                        }
                    }

                }).start();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * puts the user request into the queue to be processed
     * 
     * @param request the request metadata
     * @throws InterruptedException while pushing a new request into the queue
     * @throws NullPointerException if used after a close call
     */
    public void pushRequest(DiskRequest request) throws InterruptedException, NullPointerException {
        requestQueue.put(request);
    }

    /**
     * allocate a new page in the passed file
     * 
     * @param fileName the file which will be extended
     * @return pageId of the allocated page
     * @throws IOException          while allocating a page
     * @throws NullPointerException if used after a close call
     */
    public long allocatePage(String fileName) throws IOException, NullPointerException {
        if (!files.containsKey(fileName)) {
            DiskFile file = new RandomAccessDiskFile(storageDir + fileName);
            filesFreePages.put(fileName, new LinkedList<Long>());
            files.put(fileName, file);
            resizeLocks.put(fileName, new ReentrantLock());
            fileCount++;
        }
        //check the free page list
        Lock lock = resizeLocks.get(fileName);
        lock.lock();
        List<Long> freePages = filesFreePages.get(fileName);
        if(freePages.isEmpty()) {
            for (int i = 0; i < Globals.PRE_ALLOCATED_PAGES_COUNT; i ++) {
                freePages.add(files.get(fileName).allocatePage());
            }
        }
        long pageID = freePages.getFirst();
        freePages.removeFirst();
        lock.unlock();
        return pageID;
    }

    /**
     * 
     * @return fileCount - number of opened files curruntly held by the disk maneger
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * get the number of pages in the file
     * 
     * @param fileName
     * @return
     */
    public long getPageCount(String fileName) {
        DiskFile file = files.get(fileName);
        if (file == null) {
            return 0;
        }
        return file.getPageCnt();
    }

    /**
     * opens a file if not already opened before
     * 
     * @param fileName the name of the file to be opened
     * @throws IOException          while opening the file
     * @throws NullPointerException if used after a close call
     */
    public void open(String fileName) throws IOException, NullPointerException {
        if (files.containsKey(fileName)) {
            return;
        }

        DiskFile file = new RandomAccessDiskFile(storageDir + fileName);
        DiskFile prev = files.put(fileName, file);
        if (prev == null) {
            fileCount++;
        }
    }

}
