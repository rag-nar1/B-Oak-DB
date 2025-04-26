package test.btree;
import org.junit.Test;

import btree.Btree;
import bufferpool.*;
import diskmanager.*;
import globals.Globals;

import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.After;

public class BtreeTest {
    
    private Btree<Integer, Integer> btree;
    private BufferPool bufferPool;
    private DiskManager diskManager;
    private static final int MAX_PAGES = 3000; // Example max pages
    private static final int K = 10;
    private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
    @Before
    public void setUp() {
        CleanUp();
        // Initialize the DiskManager
        diskManager = new DiskManager();
        // Initialize the buffer pool with a size of 10 pages
        bufferPool = new BufferPool(MAX_PAGES, K, diskManager);
        // Initialize the B-tree 
        btree = new Btree<Integer, Integer>(Integer.class, Integer.class, btreeFilePath, Globals.INVALID_PAGE_ID, bufferPool);
    }

    @After
    public void CleanUp() {
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
    }

    @Test
    public void testInsertAndSearchBigWithoutSplit() {
        for (int i = 0; i < 100; i ++) {
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
    } 

    @Test
    public void testInsertAndSearchBigWithSplit() {
        int itr = 1_000_000;
        for (int i = 0; i < itr; i ++) {
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
    }

    @Test
    public void testInsertAndSearchBigRev() {
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i =0; i < itr; i ++) {
            keys[i] = i;
        }
        for (int i = 0; i < itr; i ++) {
            try {
                btree.insert(keys[i], i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }

        for (int i = 0; i < itr; i ++) {
            try {
                Integer value = btree.get(keys[i]);
                assertEquals("Search operation failed", i, value.intValue());
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }

    }

    @Test
    public void testInsertAndSearchBigRandom() {
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i = 0; i < itr; i += 2) {
            keys[i] = i;
        }
        for(int i = 1; i < itr; i += 2) {
            keys[i] = itr - i;
        }
        double startTime = (double) System.currentTimeMillis();

        for (int i = 0; i < itr; i ++) {
            try {
                btree.insert(keys[i], i);
            } catch (Exception e) {
                System.out.println(i);
                e.printStackTrace();
                fail("Insert operation failed: " + e.getMessage());
            }
        }
        double endTime = System.currentTimeMillis();
        double insertTime = (endTime - startTime) / (double)1000;
        System.out.println("insertion done in: " + insertTime + "s");

        startTime = System.currentTimeMillis();
        for (int i = 0; i < itr; i ++) {
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
        double readTime = (endTime - startTime) / (double)1000;
        System.out.println("reading done in: " + readTime + "s");
        System.out.println("test testInsertAndSearchBigRandom  done in: " + insertTime + readTime + "s");
    }

    @Test
    public void testInsertAndSearchBigRandomSwap() {
        int itr = 1_000_000;
        int[] keys = new int[itr];
        for (int i = 0; i < itr; i += 2) {
            keys[i] = i;
        }
        for(int i = 1; i < itr; i += 2) {
            keys[i] = itr - i;
        }
        double startTime = (double) System.currentTimeMillis();

        for (int i = 0; i < itr; i ++) {
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
        double fTime = (endTime - startTime) / (double)1000;
        System.out.println("test testInsertAndSearchBigRandomSwap  done in: " + fTime + "s");
    }
}
