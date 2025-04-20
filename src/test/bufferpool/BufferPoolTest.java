package test.bufferpool;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.junit.After;

import bufferpool.*;
import diskmanager.DiskManeger;
public class BufferPoolTest {
    private static final int bufferPoolSize = 1000;
    private static final int kDistance = 3;
    private static final String fileName = "test.db";
    private BufferPool bufferPool;

    @Before
    public void setUp() {
        DiskManeger diskManager = new DiskManeger();
        bufferPool = new BufferPool(bufferPoolSize, kDistance, diskManager);
    }

    @After
    public void tearDown() {
        try {
            bufferPool.close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
        // delete the test file
        java.io.File file = new java.io.File("storage/" + fileName);
        if (file.exists()) {
            if (!file.delete()) {
                fail("Failed to delete test file: " + fileName);
            }
        }

    }

    @Test
    public void testBufferPoolInitialization() {
        assertNotNull(bufferPool);
    }

    @Test
    public void testGetPage() {
        long pageId = -1;
        try {
            pageId = bufferPool.allocateNewPage(fileName);
            ReadGuard readGuard = bufferPool.getReadGuard(fileName, pageId);
            assertNotNull(readGuard);
            assertNotNull(readGuard.getData());
            readGuard.close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }

        try {
            WriteGuard writeGuard = bufferPool.getWriteGuard(fileName, pageId);
            assertNotNull(writeGuard);
            assertNotNull(writeGuard.getData());
            assertNotNull(writeGuard.getDataMut());
            writeGuard.close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testWriteRead() {
        long pageId = -1;
        byte[] msg = "Hello World".getBytes();
        try {
            pageId = bufferPool.allocateNewPage(fileName);
            WriteGuard writeGuard = bufferPool.getWriteGuard(fileName, pageId);
            byte[] data = writeGuard.getDataMut(); 
            System.arraycopy(msg, 0, data, 0, msg.length);
            writeGuard.close();
            ReadGuard readGuard = bufferPool.getReadGuard(fileName, pageId);
            ByteBuffer readData = readGuard.getData();
            byte[] readDatabuffer = new byte[msg.length];
            readData.get(readDatabuffer);
            assertArrayEquals(msg, readDatabuffer);
            readGuard.close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }
    
    @Test
    public void testDelete() {
        long pageId = -1;
        byte[] msg = "Hello World".getBytes();
        try {
            // allocate a new page
            pageId = bufferPool.allocateNewPage(fileName);
            // write data to the page
            WriteGuard writeGuard = bufferPool.getWriteGuard(fileName, pageId);
            byte[] data = writeGuard.getDataMut(); 
            System.arraycopy(msg, 0, data, 0, msg.length);
            writeGuard.close();
            // read data from the page
            ReadGuard readGuard = bufferPool.getReadGuard(fileName, pageId);
            ByteBuffer readData = readGuard.getData();
            byte[] readDatabuffer = new byte[msg.length];
            readData.get(readDatabuffer);
            assertArrayEquals(msg, readDatabuffer);
            readGuard.close();

            // delete the page
            bufferPool.deletePage(fileName, pageId);
            // try to read the deleted page
            try {
                readGuard = bufferPool.getReadGuard(fileName, pageId);
                fail("Exception should be thrown: Page not found");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("pageId is not valid"));
            }

            // try to write to the deleted page
            try {
                writeGuard = bufferPool.getWriteGuard(fileName, pageId);
                fail("Exception should be thrown: Page not found");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("pageId is not valid"));
            }

            // allocate a new page
            long newPageId = bufferPool.allocateNewPage(fileName);
            assertEquals(pageId, newPageId);
            // write data to the new page
            msg = "Hello World Again".getBytes();
            writeGuard = bufferPool.getWriteGuard(fileName, newPageId);
            data = writeGuard.getDataMut();
            System.arraycopy(msg, 0, data, 0, msg.length);
            writeGuard.close();

            // read data from the new page
            readGuard = bufferPool.getReadGuard(fileName, newPageId);
            readData = readGuard.getData();
            readDatabuffer = new byte[msg.length];
            readData.get(readDatabuffer);
            assertArrayEquals(msg, readDatabuffer);
            readGuard.close();
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testEviction() {
        int pageCount = 2 * bufferPoolSize;
        // allocate pages
        for (int i = 0; i < pageCount; i++) {
            try {
                long pageId = bufferPool.allocateNewPage(fileName);
                assertEquals(i, pageId);
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
        
        // check if the pages are evicted correctly
        for (int i = 0; i < pageCount; i++) {
            byte[] msg = ("Hello World " + i + "\0").getBytes();
            try {
                WriteGuard writeGuard = bufferPool.getWriteGuard(fileName, i);
                byte[] data = writeGuard.getDataMut();
                System.arraycopy(msg, 0, data, 0, msg.length);
                writeGuard.close();
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }

        // check if the pages are evicted correctly
        for(int round = 0; round <= kDistance; round++) {
            for (int i = 0; i < pageCount; i++) {
                String msg = ("Hello World " + i + "\0");
                try {
                    ReadGuard readGuard = bufferPool.getReadGuard(fileName, i);
                    ByteBuffer readData = readGuard.getData();
                    byte[] readDatabuffer = new byte[readData.remaining()];
                    readData.get(readDatabuffer);
                    String readMsg = new String(readDatabuffer);
                    assertTrue(readMsg.startsWith(msg));
                    assertTrue(readMsg.length() > msg.length());
                    // check if the message is correct
                    assertTrue(readMsg.contains(msg));
                    readGuard.close();
                } catch (Exception e) {
                    fail("Exception should not be thrown: " + e.getMessage());
                }
            }
        }

    }

    class Writer implements Runnable {
        private String fileName;
        private long pageId;
        private byte[] msg;

        public Writer(String fileName, long pageId, byte[] msg) {
            this.fileName = fileName;
            this.pageId = pageId;
            this.msg = msg;
        }

        public void run() {
            try {
                WriteGuard writeGuard = bufferPool.getWriteGuard(fileName, pageId);
                while(writeGuard == null) { // ensure we get a write guard so each page is written
                    writeGuard = bufferPool.getWriteGuard(fileName, pageId);
                }
                byte[] data = writeGuard.getDataMut();
                System.arraycopy(msg, 0, data, 0, msg.length);
                writeGuard.close();
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
    } 

    class Reader implements Runnable {
        private String fileName;
        private long pageId;

        public Reader(String fileName, long pageId) {
            this.fileName = fileName;
            this.pageId = pageId;
        }

        public void run() {
            try {
                // wait for a short time to allow the writer thread to finish
                ReadGuard readGuard = bufferPool.getReadGuard(fileName, pageId);
                while (readGuard == null) { // it is ok to be null just return
                    readGuard = bufferPool.getReadGuard(fileName, pageId);
                }
                ByteBuffer readData = readGuard.getData();
                byte[] readDatabuffer = new byte[readData.remaining()];
                readData.get(readDatabuffer);
                readGuard.close();
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
    }

    @Test
    public void testConcurrency() {
        int pageCount = 2 * bufferPoolSize;
        // allocate pages
        for (int i = 0; i < pageCount; i++) {
            try {
                long pageId = bufferPool.allocateNewPage(fileName);
                assertEquals(i, pageId);
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
        
        List<Thread> threads = new ArrayList<>();
        
        for(int i = 0; i < pageCount; i++) {
            byte[] msg = ("Hello World " + i + "\0").getBytes();
            // writer thread
            Thread curr1 = new Thread(new Writer(fileName, i, msg));
            threads.add(curr1);
            Thread curr2 = new Thread(new Reader(fileName, i));
            threads.add(curr2);

        }

        // shuffle the threads
        Collections.shuffle(threads);
        // start the threads
        for (Thread thread : threads) {
            thread.start();
        }

        // wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
        
        // check if all pages are written correctly
        for (int i = 0; i < pageCount; i++) {
            String msg = ("Hello World " + i + "\0");
            try {
                ReadGuard readGuard = bufferPool.getReadGuard(fileName, i);
                ByteBuffer readData = readGuard.getData();
                byte[] readDatabuffer = new byte[readData.remaining()];
                readData.get(readDatabuffer);
                // has prefix "Hello World " + i
                String readMsg = new String(readDatabuffer);
                assertTrue(readMsg.startsWith(msg));
                assertTrue(readMsg.length() > msg.length());
                // check if the message is correct
                assertTrue(readMsg.contains(msg));
                readGuard.close();
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
    } 

    @Test
    public void testConcurrencyBig() {
        int pageCount = 2 * bufferPoolSize;
        // allocate pages
        for (int i = 0; i < pageCount; i++) {
            try {
                long pageId = bufferPool.allocateNewPage(fileName);
                assertEquals(i, pageId);
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
        
        List<Thread> threads = new ArrayList<>();
        
        for(int i = 0; i < pageCount; i++) {
            byte[] msg = ("Hello World " + i + "\0").getBytes();
            // writer thread
            Thread curr1 = new Thread(new Writer(fileName, i, msg));
            threads.add(curr1);
            Thread curr2 = new Thread(new Reader(fileName, i));
            threads.add(curr2);
            curr1 = new Thread(new Writer(fileName, i, msg));
            threads.add(curr1);
            curr2 = new Thread(new Reader(fileName, i));
            threads.add(curr2);

        }

        // shuffle the threads
        Collections.shuffle(threads);
        // start the threads
        for (Thread thread : threads) {
            thread.start();
        }

        // wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
        
        // check if all pages are written correctly
        for (int i = 0; i < pageCount; i++) {
            String msg = ("Hello World " + i + "\0");
            try {
                ReadGuard readGuard = bufferPool.getReadGuard(fileName, i);
                ByteBuffer readData = readGuard.getData();
                byte[] readDatabuffer = new byte[readData.remaining()];
                readData.get(readDatabuffer);
                // has prefix "Hello World " + i
                String readMsg = new String(readDatabuffer);
                assertTrue(readMsg.startsWith(msg));
                assertTrue(readMsg.length() > msg.length());
                // check if the message is correct
                assertTrue(readMsg.contains(msg));
                readGuard.close();
            } catch (Exception e) {
                fail("Exception should not be thrown: " + e.getMessage());
            }
        }
    }

}
