
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import diskmanager.DiskManeger;
import bufferpool.BufferPool;
import bufferpool.ReadGuard;
import bufferpool.Replacer;
import bufferpool.WriteGuard;

public class Main {
    public static void main(String[] args)  throws Exception, InterruptedException, NullPointerException, ExecutionException {
        // Create storage directory if it doesn't exist
        File dir = new File("storage");
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        String fileName = "file1.db";
       
        DiskManeger DM = new DiskManeger();
        Replacer replacer = new Replacer(10);
        BufferPool bufferPool = new BufferPool(10, replacer, DM);

        long pageId1 = bufferPool.allocateNewPage(fileName);
        long pageId2 = bufferPool.allocateNewPage(fileName);

        WriteGuard writeGuard1 = bufferPool.getWriteGuard(fileName, pageId1);
        WriteGuard writeGuard2 = bufferPool.getWriteGuard(fileName, pageId2);

        byte[] data1 = writeGuard1.getDataMut();
        byte[] data2 = writeGuard2.getDataMut();

        byte[] msg1 = "Hello, page 1".getBytes();
        byte[] msg2 = "Hello, page 2".getBytes();

        System.arraycopy(msg1, 0, data1, 0, msg1.length);
        System.arraycopy(msg2, 0, data2, 0, msg2.length);

        writeGuard1.close();
        writeGuard2.close();

        ReadGuard readGuard1 = bufferPool.getReadGuard(fileName, pageId1);
        ReadGuard readGuard2 = bufferPool.getReadGuard(fileName, pageId2);

        ByteBuffer readData1 = readGuard1.getData();
        ByteBuffer readData2 = readGuard2.getData();

        for (int i = 0; i < msg1.length; i++) {
            System.out.print((char) readData1.get(i));
        }
        System.out.println();
        for (int i = 0; i < msg2.length; i++) {
            System.out.print((char) readData2.get(i));
        }
        System.out.println();
        readGuard1.close();
        readGuard2.close();
        bufferPool.close();
    }
}
