package test.btree;

import org.junit.Test;

import btree.Btree;
import btree.Cursor;
import bufferpool.*;
import diskmanager.*;
import globals.Globals;

import org.junit.Before;
import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;

public class BtreeTest {

    private Btree<Integer, Integer> btree;
    private BufferPool bufferPool;
    private DiskManager diskManager;
    private static final int MAX_PAGES = 4000; // Example max pages
    private static final int K = 10;
    private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
    // private static final String logsFilePath = "logs.btree"; // Example file path for the B-tree
    // private Thread monitor;

    @Before
    public void setUp() {
        CleanUp();
        // Initialize the DiskManager
        diskManager = new DiskManager();
        // Initialize the buffer pool with a size of 10 pages
        bufferPool = new BufferPool(MAX_PAGES, K, diskManager);
        // Initialize the B-tree
        btree = new Btree<Integer, Integer>(Integer.class, Integer.class, btreeFilePath, Globals.INVALID_PAGE_ID,
                bufferPool);
        // monitor = new Thread(() -> {
        //     ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
        //     try {
        //         while (!Thread.currentThread().isInterrupted()) {
        //                 long[] ids = tmxb.findDeadlockedThreads();
        //                 if (ids != null) {
        //                     System.err.println("=== Deadlocked Threads ===");
        //                     ThreadInfo[] threadInfos = tmxb.getThreadInfo(ids, true, true);
        //                     if (threadInfos != null) {
        //                         for (ThreadInfo info : threadInfos) {
        //                             System.err.printf("Thread %s (id=%d) waiting for %s held by %s%n",
        //                                             info.getThreadName(),
        //                                             info.getThreadId(),
        //                                             info.getLockName(),
        //                                             info.getLockOwnerName());
        //                             for (StackTraceElement ste : info.getStackTrace()) {
        //                                 System.err.println("\t at " + ste);
        //                             }
        //                         }
        //                     }
        //                 }
        //                 return;
        //             }
        //             Thread.sleep(50);
        //     } catch (InterruptedException ignored) {
        //         // exit
        //     }
        // }, "Deadlock-Monitor");
        // monitor.start();
    }

    @After
    public void CleanUp() {
        // monitor.interrupt();
        // delete the test file
        java.io.File file = new java.io.File("storage/" + btreeFilePath);
        if (file.exists()) {
            if (!file.delete()) {
                fail("Failed to delete test file: " + btreeFilePath);
            }
        }
    }

    @Test
    public void testInsertAndSearch() {
        double startTime = (double) System.currentTimeMillis();
        int key = 5;
        int value = 10;
        try {
            btree.insert(key, value);
        } catch (Exception e) {
            fail("Insert operation failed: " + e.getMessage());
        }
        try {
            Integer result = btree.get(key);
            assertEquals("Search operation failed", value, result.intValue());
        } catch (Exception e) {
            fail("Search operation failed: " + e.getMessage());
        }

        double endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testInsertAndSearch done in: " + fTime + "s");

    }

    @Test
    public void testInsertAndSearchBigWithoutSplit() {
        double startTime = (double) System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            try {
                btree.insert(i, i);
            } catch (Exception e) {
                fail("Insert operation failed: " + e.getMessage());
            }
            try {
                Integer result = btree.get(i);
                assertEquals("Search operation failed", i, result.intValue());
            } catch (Exception e) {
                fail("Search operation failed: " + e.getMessage());
            }
        }
        double endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testInsertAndSearchBigWithoutSplit done in: " + fTime + "s");
    }

    @Test
    public void testInsertAndSearchBigWithSplit() {
        double startTime = (double) System.currentTimeMillis();
        int itr = 1_000_000;
        for (int i = 0; i < itr; i++) {
            try {
                btree.insert(i, i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
            try {
                Integer result = btree.get(i);
                assertEquals("Search operation failed", i, result.intValue());
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Search operation failed: " + e.getMessage());
            }
        }
        double endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testInsertAndSearchBigWithSplit done in: " + fTime + "s");
    }

    @Test
    public void testInsertAndSearchBigRev() {
        double startTime = (double) System.currentTimeMillis();
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i = 0; i < itr; i++) {
            keys[i] = i;
        }
        for (int i = 0; i < itr; i++) {
            try {
                btree.insert(keys[i], i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }

        for (int i = 0; i < itr; i++) {
            try {
                Integer value = btree.get(keys[i]);
                assertEquals("Search operation failed", i, value.intValue());
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }

        double endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testInsertAndSearchBigRev done in: " + fTime + "s");

    }

    @Test
    public void testInsertAndSearchBigRandom() {
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i = 0; i < itr; i += 2) {
            keys[i] = i;
        }
        for (int i = 1; i < itr; i += 2) {
            keys[i] = itr - i;
        }
        double startTime = (double) System.currentTimeMillis();

        for (int i = 0; i < itr; i++) {
            try {
                btree.insert(keys[i], i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }
        double endTime = System.currentTimeMillis();
        double insertTime = (endTime - startTime) / (double) 1000;
        System.out.println("insertion done in: " + insertTime + "s");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < itr; i++) {
            try {
                Integer value = btree.get(keys[i]);
                assertEquals("Search operation failed", i, value.intValue());
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }
        endTime = System.currentTimeMillis();
        double readTime = (endTime - startTime) / (double) 1000;
        System.out.println("reading done in: " + readTime + "s");
        readTime += insertTime;
        System.out.println("test testInsertAndSearchBigRandom  done in: " + readTime + "s");
    }

    @Test
    public void testInsertAndSearchBigRandomSwap() {
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i = 0; i < itr; i += 2) {
            keys[i] = i;
        }
        for (int i = 1; i < itr; i += 2) {
            keys[i] = itr - i;
        }
        double startTime = (double) System.currentTimeMillis();

        for (int i = 0; i < itr; i++) {
            try {
                btree.insert(keys[i], i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
            try {
                Integer value = btree.get(keys[i]);
                assertEquals("Search operation failed", i, value.intValue());
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }
        double endTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testInsertAndSearchBigRandomSwap  done in: " + fTime + "s");
    }

    @Test
    public void testCursur() throws Exception {
        int itr = 1_000_000;
        double startTime = (double) System.currentTimeMillis();

        for (int i = 0; i < itr; i++) {
            try {
                btree.insert(i, i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }

        int i = 0;
        try {
            for (Cursor<Integer, Integer> cursor = btree.begin(); !cursor.isEnd(); cursor.next()) {
                Cursor<Integer, Integer>.Pair<Integer, Integer> curr = cursor.get();
                assertEquals(i, curr.first.intValue());
                assertEquals(i, curr.second.intValue());
                i++;
            }

            double endTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis();
            double fTime = (endTime - startTime) / (double) 1000;
            System.out.println("test testCursur done in: " + fTime + "s");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testConcurrency() throws Exception {
        int itrs = 20;
        for (int itr = 1; itr <= itrs; itr ++) {
            setUp();
            double startTime = (double) System.currentTimeMillis();

            int writersCnt = 100;
            int readersCnt = 100;
            List<Thread> threads = new ArrayList<>();
            int op = 10000;
            for (int i = 0; i < writersCnt; i++) {
                final int end = op * i;
                Thread writer = new Thread(() -> {
                    for (int key = end - op; key < end; key++) {
                        try {
                            btree.insert(key, key);
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    }
                });
                threads.add(writer);
            }

            for(Thread thread: threads) {
                thread.start();
            }

            for(Thread thread: threads) {
                thread.join();
            }
            threads = new ArrayList<>();

            for (int i = 0; i < readersCnt; i++) {
                final int end = op * i;
                Thread reader = new Thread(() -> {
                    for (int key = end - op; key < end; key++) {
                        try {
                            int val = btree.get(key).intValue();
                            assertEquals(key, val);
                        } catch (Exception e) {
                            System.out.println("thread " + end / op + ": expected ->" + key);
                            e.printStackTrace();
                            fail();
                        }
                    }
                });
                threads.add(reader);
            }

            for(Thread thread: threads) {
                thread.start();
            }

            for(Thread thread: threads) {
                thread.join();
            }

            double endTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis();
            double fTime = (endTime - startTime) / (double) 1000;
            System.out.println("test testConcurrency done itr "+ itr +" : " + fTime + "s");
        }
    }
}
