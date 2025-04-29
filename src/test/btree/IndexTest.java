package test.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import btree.Btree;
import bufferpool.BufferPool;
import diskmanager.DiskManager;
import globals.Globals;

public class IndexTest {
    class Key implements Comparable<Key> {
        int id;
        int age;
        public Key(int id, int age) {
            this.id = id;
            this.age = age;
        }
        public int compareTo(Key rhs) {
            if (this.id < rhs.id) {
                return -1;
            }

            if (this.id > rhs.id) {
                return 1;
            }

            if (this.age < rhs.age) {
                return -1;
            }

            if (this.age > rhs.age) {
                return -1;
            }
            return 0;
        }
    }

    private Btree<Key, Long> btree;
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
        btree = new Btree<Key, Long>(Key.class, Long.class, btreeFilePath, Globals.INVALID_PAGE_ID,
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

     @Test
    public void testCompositeKey() throws Exception {
        int keysnumber = 1_000;
        for (int i = 0; i < keysnumber; i ++) {
            Key key = new Key(i, keysnumber - i);
            try {
                btree.insert(key, (long) i);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        for (int i = 0; i < keysnumber; i ++) {
            Key key = new Key(i, keysnumber - i);
            try {
                long val = btree.get(key);
                assertEquals(i, val);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }
    }
}
