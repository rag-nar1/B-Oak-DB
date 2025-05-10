package test.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import btree.Btree;
import bufferpool.BufferPool;
import diskmanager.BasicDiskManager;
import diskmanager.DiskManager;
import globals.Globals;
import types.Compositekey;
import types.Template;

public class BtreeBenchmark {
    private Btree btree;
    private BufferPool bufferPool;
    private DiskManager diskManager;
    private static final int MAX_PAGES = 4000; // Example max pages
    private static final int K = 30;
    private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
    Template keyType;
    Template valueType;

    @Before
    public void setUp() {
        CleanUp();
        // Initialize the DiskManager
        diskManager = new BasicDiskManager();
        // Initialize the buffer pool with a size of 10 pages
        bufferPool = new BufferPool(MAX_PAGES, K, diskManager);
        keyType = new Template(Integer.class, Integer.class);
        valueType = new Template(Integer.class);
        // Initialize the B-tree
        btree = new Btree(keyType, valueType, btreeFilePath, Globals.INVALID_PAGE_ID,
                bufferPool);
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

    private Compositekey makeCompositekey(int age, int salary, Template type) {
        Compositekey key = new Compositekey(type);
        key.set(0, age, Integer.class);
        key.set(1, salary, Integer.class);
        return key;
    }

    private Compositekey makeCompositekey(int val, Template type) {
        Compositekey key = new Compositekey(type);
        key.set(0, val, Integer.class);
        return key;
    }

    @Test
    public void testBtreeBenchmark() throws Exception {
        
        int keysnumber = 2_000_000;
        setUp();
        double startTime = (double) System.currentTimeMillis();
        double[] insertTimes = new double[keysnumber];
        double[] searchTimes = new double[keysnumber];

        int writersCnt = 200;
        int readersCnt = 200;
        List<Thread> threads = new ArrayList<>();
        int op = 10000;
        for (int i = 0; i < writersCnt; i++) {
            final int end = op * (i + 1);
            Thread writer = new Thread(() -> {
                for (int key = end - op; key < end; key++) {
                    try {
                        long insertStartTime = System.currentTimeMillis();
                        btree.insert(makeCompositekey(key, keysnumber - key, keyType),
                                makeCompositekey(key, valueType));
                        long insertEndTime = System.currentTimeMillis();
                        insertTimes[key] = (insertEndTime - insertStartTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            });
            threads.add(writer);
        }

        Collections.shuffle(threads);
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        // insert total time
        double insertTotalTime = System.currentTimeMillis() - startTime;
        threads = new ArrayList<>();
        double startTime2 = System.currentTimeMillis();
        for (int i = 0; i < readersCnt; i++) {
            final int end = op * (i + 1);
            Thread reader = new Thread(() -> {
                for (int key = end - op; key < end; key++) {
                    try {
                        long searchStartTime = System.currentTimeMillis();
                        Compositekey val = btree.get(makeCompositekey(key, keysnumber - key, keyType));
                        long searchEndTime = System.currentTimeMillis();
                        searchTimes[key] = (searchEndTime - searchStartTime);
                        assertEquals(0, val.compareTo(makeCompositekey(key, valueType)));
                    } catch (Exception e) {
                        System.out.println("thread " + end / op + ": expected ->" + key);
                        e.printStackTrace();
                        fail();
                    }
                }
            });
            threads.add(reader);
        }

        Collections.shuffle(threads);
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
        // search total time
        double searchTotalTime = System.currentTimeMillis() - startTime2;
        double endTime = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("┌───────────────────────────────────────────────────────────");
        System.out.println("\033[1;36m                   B-TREE BENCHMARK RESULTS                  \033[0m");
        System.out.println("┬───────────────────────────────────────────────────────────");
        System.out.printf(" \033[1mTotal execution time:\033[0m      %8.2f seconds\n", fTime);
        System.out.println("┼───────────────────────────────────────────────────────────");
        System.out.printf(" \033[1;32mInsert operations:\033[0m\n");
        System.out.printf("   - Total time:            %8.2f seconds\n", insertTotalTime / 1000);
        System.out.printf("   - Average time:          %8.2f ms per operation\n", Arrays.stream(insertTimes).average().getAsDouble());
        System.out.printf("   - Throughput:            %8.2f operations per second\n", keysnumber / insertTotalTime * 1000);
        System.out.println("┼───────────────────────────────────────────────────────────");
        System.out.printf(" \033[1;33mSearch operations:\033[0m\n");
        System.out.printf("   - Total time:            %8.2f seconds\n", searchTotalTime / 1000);
        System.out.printf("   - Average time:          %8.2f ms per operation\n", Arrays.stream(searchTimes).average().getAsDouble());
        System.out.printf("   - Throughput:            %8.2f operations per second\n", keysnumber / searchTotalTime * 1000);
        System.out.println("┼───────────────────────────────────────────────────────────");
        System.out.printf(" \033[1;35mOverall throughput:\033[0m        %8.2f operations per second\n", (keysnumber * 2) / (insertTotalTime + searchTotalTime) * 1000);
        System.out.println("└───────────────────────────────────────────────────────────");
    }
}