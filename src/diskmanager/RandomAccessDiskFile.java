package diskmanager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import globals.Globals;

public class RandomAccessDiskFile implements DiskFile {

    private Path filePath;
    private RandomAccessFile file; // file length always will be a multiply of pageSize
    private FileChannel channel;
    private long pageSize; // page size in kb
    private long pageCnt; // number of pages in the file
    private long fileSize;

    public RandomAccessDiskFile(String filePath) throws IOException {
        this.filePath = Paths.get(filePath);
        try { // try to open file
            this.file = new RandomAccessFile(filePath, "rw");
            this.channel = file.getChannel();
        } catch (FileNotFoundException e) { // create the file if does not exist
            Files.createFile(this.filePath);
            this.file = new RandomAccessFile(filePath, "rw");
            this.channel = file.getChannel();
        }

        this.pageSize = Globals.PAGE_SIZE; // page size in bytes
        init();
    }

    public void close() throws IOException {
        file.close();
    }

    private void init() throws IOException {
        fileSize = file.length();
        pageCnt = fileSize / pageSize;
    }

    public synchronized long allocatePage() throws IOException { // allocate a page and returns it's id
        file.seek(fileSize);
        byte[] data = new byte[Globals.PAGE_SIZE];
        file.write(data);
        pageCnt++;
        fileSize += pageSize;
        return pageCnt - 1;
    }

    public byte[] readPage(long pageID) throws IOException {
        if (pageID >= pageCnt) {
            throw new IOException("pageId: " + pageID);
        }

        byte[] buffer = new byte[(int) pageSize];
        int read = channel.read(ByteBuffer.wrap(buffer), pageID * pageSize);
        if (read != pageSize) {
            throw new IOException();
        }
        return buffer;
    }

    public void readPage(long pageID, byte[] dst) throws IOException {
        if (pageID >= pageCnt) {
            throw new IOException("pageId: " + pageID);
        }

        int read = channel.read(ByteBuffer.wrap(dst), pageID * pageSize);
        if (read != pageSize) {
            throw new IOException();
        }
    }

    public void writePage(long pageID, byte[] src) throws IOException {
        int wrote = channel.write(ByteBuffer.wrap(src), pageID * pageSize);
        if (wrote != pageSize) {
            throw new IOException();
        }
    }

    // geters
    public long getPageCnt() {
        return pageCnt;
    }

    public long getFileSize() {
        return fileSize;
    }
}