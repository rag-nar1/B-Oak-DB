package diskmanager;

import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Boolean> getFuture() {
        return finish;
    }

    public byte[] getData() {
        return data;
    }
}
