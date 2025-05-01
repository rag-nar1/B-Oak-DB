package test.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.security.auth.kerberos.KeyTab;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import btree.Btree;
import bufferpool.BufferPool;
import diskmanager.DiskManager;
import globals.Globals;
import types.Compositekey;
import types.Template;

public class IndexTest {

   private Btree btree;
    private BufferPool bufferPool;
    private DiskManager diskManager;
    private static final int MAX_PAGES = 4000; // Example max pages
    private static final int K = 10;
    private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
    Template keyType;
    Template valueType;

    @Before
    public void setUp() {
        CleanUp();
        // Initialize the DiskManager
        diskManager = new DiskManager();
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
        for (int i = 0; i < keysnumber; i ++) {
            try {
                btree.insert(makeCompositekey(i, keysnumber - i, keyType), makeCompositekey(i, valueType));
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        for (int i = 0; i < keysnumber; i ++) {
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
}
