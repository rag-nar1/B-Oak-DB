package test.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
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

public class IndexTest {

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
    public void testCompositeKey() throws Exception {
        double startTime = (double) System.currentTimeMillis();
        int keysnumber = 1_000_000;
        for (int i = 0; i < keysnumber; i++) {
            try {
                btree.insert(makeCompositekey(i, keysnumber - i, keyType), makeCompositekey(i, valueType));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        for (int i = 0; i < keysnumber; i++) {
            try {
                Compositekey val = btree.get(makeCompositekey(i, keysnumber - i, keyType));
                assertEquals(0, val.compareTo(makeCompositekey(i, valueType)));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
        double endTime = System.currentTimeMillis();
        double fTime = (endTime - startTime) / (double) 1000;
        System.out.println("test testCompositeKey done in: " + fTime + "s");
    }

    @Test
    public void testConcurrencyRand() throws Exception {
        int itrs = 1;
        int keysnumber = 1_000_000;
        for (int itr = 1; itr <= itrs; itr++) {
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
                            btree.insert(makeCompositekey(key, keysnumber - key, keyType),
                                    makeCompositekey(key, valueType));
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
            threads = new ArrayList<>();

            for (int i = 0; i < readersCnt; i++) {
                final int end = op * i;
                Thread reader = new Thread(() -> {
                    for (int key = end - op; key < end; key++) {
                        try {
                            Compositekey val = btree.get(makeCompositekey(key, keysnumber - key, keyType));
                            if(val.compareTo(makeCompositekey(key, valueType)) != 0) {
                                System.out.println("found :" + val.<Integer>getVal(0));
                                System.out.println("expected :" + key);
                            }  
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

            double endTime = System.currentTimeMillis();
            endTime = System.currentTimeMillis();
            double fTime = (endTime - startTime) / (double) 1000;
            System.out.println("test testConcurrency done itr " + itr + " : " + fTime +
                    "s");
        }
    }
}
