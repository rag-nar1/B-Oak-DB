package diskmanager;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.Closeable;
import java.util.concurrent.BlockingQueue;

public class DiskManeger implements Closeable {
    public class DiskRequest {
        String fileName;
        long pageID;
        byte[] data;
        boolean isWrite;
        CompletableFuture<Boolean> finish;
        public DiskRequest(String fileName, long pageID, byte[] data, boolean isWrite) {
            this.fileName = fileName;
            this.pageID = pageID;
            this.data = data;
            this.isWrite = isWrite;
            this.finish = new CompletableFuture<Boolean>();
        }

    }

    
    private Map<String, DiskFile> files;
    private int fileCount;
    private BlockingQueue<DiskRequest> requestQueue;

    /**
     * defualt constructre
     * todo: make new constructors based on a config 
     */
    public DiskManeger() {
        this.files = new ConcurrentHashMap<>();
        this.fileCount = 0;
        this.requestQueue = new LinkedBlockingQueue<DiskRequest>(100); // capped to 100 requests
        new Thread(() -> { // start the worker that would fetch the requests and start a thread for each
            try {
                run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                e.printStackTrace(); // Log the exception
            }
        }).start();
    }

    /**
     * closes all files and invalidate the requestQueue and the files map
     * used as a destractor 
     * @throws NullPointerException if the object used after calling the close method for the first time
     */
    public void close() throws NullPointerException {
        requestQueue.clear();
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

    }

    /**
     * the main threads run here start by fetching a request from the queue spin a thread to hundle it
     * @throws InterruptedException while ferching a request
     * @throws NullPointerException if used after a close call
     */
    private void run() throws InterruptedException, NullPointerException{
        close();
        while(true) {
            DiskRequest currentRequest = requestQueue.take();
            new Thread(()-> {
                DiskFile file = this.files.get(currentRequest.fileName);
                if (file == null) {
                    return;
                }

                if (currentRequest.isWrite) {
                    try {
                        file.writePage(currentRequest.pageID, currentRequest.data);
                        currentRequest.finish.complete(true);
                    } catch (IOException e) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        e.printStackTrace(); // Log the exception
                    }
                } else {
                    try {
                        file.readPage(currentRequest.pageID, currentRequest.data);
                        currentRequest.finish.complete(true);
                    } catch (IOException e) {
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        e.printStackTrace(); // Log the exception
                    }
                }

            }).start();
        }
    }

    /**
     * puts the user request into the queue to be processed  
     * @param request the request metadata 
     * @throws InterruptedException while pushing a new request into the queue
     * @throws NullPointerException if used after a close call
     */
    public void pushRequest(DiskRequest request) throws InterruptedException, NullPointerException {
        requestQueue.put(request);
    }

    /**
     * allocate a new page in the passed file
     * @param fileName the file which will be extended 
     * @return  pageId of the allocated page
     * @throws IOException while allocating a page
     * @throws NullPointerException if used after a close call
     */
    public long allocatePage(String fileName) throws IOException, NullPointerException {
        DiskFile file = files.get(fileName);
        if (file == null) {
            return -1;
        }

        return file.allocatePage();
    }

    /**
     * 
     * @return fileCount - number of opened files curruntly held by the disk maneger 
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * opens a file if not already opened before
     * @param fileName the name of the file to be opened
     * @throws IOException while opening the file
     * @throws NullPointerException if used after a close call
     */
    public void open(String fileName) throws IOException, NullPointerException {
        if (files.containsKey(fileName)) {
            return;
        }

        DiskFile file = new DiskFile(fileName, 4096);
        DiskFile prev = files.put(fileName, file);
        if (prev == null) {
            fileCount ++;
        }
    }
    
}
