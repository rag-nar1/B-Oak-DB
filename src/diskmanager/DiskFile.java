package diskmanager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.io.Closeable;
import java.nio.file.Path;

public class DiskFile implements Closeable {

    private Path filePath;
    private RandomAccessFile file; // file length always will be a multiply of pageSize
    private long pageSize; // page size in kb
    private long pageCnt; // number of pages in the file
    private long fileSize;

    public DiskFile(String filePath, long pageSize) throws IOException {
        this.filePath = Paths.get(filePath);
        try { // try to open file
            this.file = new RandomAccessFile(filePath,"rw");
        } catch (FileNotFoundException e) { // create the file if does not exist
            Files.createFile(this.filePath);
            this.file = new RandomAccessFile(filePath,"rw");
        } 

        this.pageSize = pageSize;
        init();
    }

    public void close() throws IOException {
        file.close();
    }

    private void init() throws IOException {
        fileSize = file.length();
        pageCnt = fileSize / pageSize;
    }

    public long allocatePage() throws IOException { // allocate a page and returns it's id
        file.setLength(fileSize + pageSize);
        pageCnt ++;
        fileSize += pageSize;
        return pageCnt - 1;
    }

    public byte[] readPage(long pageID) throws IOException {
        file.seek(pageID * pageSize);
        byte[] data = new byte[(int)pageSize];
        int readBytes = file.read(data);
        if (readBytes != (int)pageSize) {
            throw new IOException();
        }
        return data;
    }

    public void readPage(long pageID, byte[] data) throws IOException {
        file.seek(pageID * pageSize);
        int readBytes = file.read(data);
        if (readBytes != (int)pageSize) {
            throw new IOException();
        }
    }

    public void writePage(long pageID, byte[] data) throws IOException {
        file.seek(pageID * pageSize);
        file.write(data);
    }

    // geters
    public long getPageCnt() {
        return pageCnt;
    }

    public long getFileSize() {
        return fileSize;
    }
}