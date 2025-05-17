package test.btree;

import static org.junit.Assert.*;

import btree.Btree;
import btree.Cursor;
import bufferpool.*;
import diskmanager.*;
import globals.Globals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import types.Compositekey;
import types.Template;

public class BtreeTest {

  private Btree btree;
  private BufferPool bufferPool;
  private DiskManager diskManager;
  private static final int MAX_PAGES = 4000; // Example max pages
  private static final int K = 10;
  private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
  Template keyType;
  Template valueType;

  // private static final String logsFilePath = "logs.btree"; // Example file path
  // for the B-tree
  // private Thread monitor;

  @Before
  public void setUp() {
    CleanUp();
    // Initialize the DiskManager
    diskManager = new BasicDiskManager();
    // Initialize the buffer pool with a size of 10 pages
    bufferPool = new BufferPool(MAX_PAGES, K, diskManager);
    // Initialize the B-tree
    keyType = new Template(Integer.class);
    valueType = new Template(Integer.class);
    btree = new Btree(keyType, valueType, btreeFilePath, Globals.INVALID_PAGE_ID, bufferPool);
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

  private Compositekey makeCompositekey(int val, Template type) {
    Compositekey key = new Compositekey(type);
    key.set(0, val, Integer.class);
    return key;
  }

  @Test
  public void testInsertAndSearch() {
    double startTime = (double) System.currentTimeMillis();
    int key = 5;
    int value = 10;
    try {
      btree.insert(makeCompositekey(key, keyType), makeCompositekey(value, valueType));
    } catch (Exception e) {
      fail("Insert operation failed: " + e.getMessage());
    }
    try {
      Compositekey result = btree.get(makeCompositekey(key, keyType));
      assertEquals(
          "Search operation failed", 0, result.compareTo(makeCompositekey(value, valueType)));
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
        btree.insert(makeCompositekey(i, keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        fail("Insert operation failed: " + e.getMessage());
      }
      try {
        Compositekey result = btree.get(makeCompositekey(i, keyType));
        assertEquals(
            "Search operation failed", 0, result.compareTo(makeCompositekey(i, valueType)));
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
        btree.insert(makeCompositekey(i, keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        System.out.println(i);
        e.printStackTrace();
        fail("Insert operation failed: " + e.getMessage());
      }
      try {
        Compositekey result = btree.get(makeCompositekey(i, keyType));
        assertEquals(
            "Search operation failed", 0, result.compareTo(makeCompositekey(i, valueType)));
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
        btree.insert(makeCompositekey(keys[i], keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        System.out.println(i);
        e.printStackTrace();
        fail("Insert operation failed: " + e.getMessage());
      }
    }

    for (int i = 0; i < itr; i++) {
      try {
        Compositekey result = btree.get(makeCompositekey(keys[i], keyType));
        assertEquals(
            "Search operation failed", 0, result.compareTo(makeCompositekey(i, valueType)));
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
        btree.insert(makeCompositekey(keys[i], keyType), makeCompositekey(i, valueType));
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
        Compositekey result = btree.get(makeCompositekey(keys[i], keyType));
        assertEquals(
            "Search operation failed", 0, result.compareTo(makeCompositekey(i, valueType)));
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
        btree.insert(makeCompositekey(keys[i], keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        System.out.println(i);
        e.printStackTrace();
        fail("Insert operation failed: " + e.getMessage());
      }
      try {
        Compositekey result = btree.get(makeCompositekey(keys[i], keyType));
        assertEquals(
            "Search operation failed", 0, result.compareTo(makeCompositekey(i, valueType)));
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
  public void testCursor() throws Exception {
    int itr = 1_000_000;
    double startTime = (double) System.currentTimeMillis();

    for (int i = 0; i < itr; i++) {
      try {
        btree.insert(makeCompositekey(i, keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        System.out.println(i);
        e.printStackTrace();
        fail("Insert operation failed: " + e.getMessage());
      }
    }

    int i = 0;
    try {
      for (Cursor cursor = btree.begin(); !cursor.isEnd(); cursor.next()) {
        Cursor.Pair<Compositekey, Compositekey> curr = cursor.get();
        assertEquals(
            "Search operation failed", 0, curr.first.compareTo(makeCompositekey(i, keyType)));
        assertEquals(
            "Search operation failed", 0, curr.second.compareTo(makeCompositekey(i, valueType)));
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
    int itrs = 5;
    for (int itr = 1; itr <= itrs; itr++) {
      setUp();
      double startTime = (double) System.currentTimeMillis();

      int writersCnt = 100;
      int readersCnt = 100;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * i;
        Thread writer = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  btree.insert(
                      makeCompositekey(key, keyType), makeCompositekey(key, valueType));
                } catch (Exception e) {
                  e.printStackTrace();
                  fail();
                }
              }
            });
        threads.add(writer);
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }
      threads = new ArrayList<>();

      for (int i = 0; i < readersCnt; i++) {
        final int end = op * i;
        Thread reader = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  Compositekey result = btree.get(makeCompositekey(key, keyType));
                  assertEquals(
                      "Search operation failed",
                      0,
                      result.compareTo(makeCompositekey(key, valueType)));
                } catch (Exception e) {
                  System.out.println("thread " + end / op + ": expected ->" + key);
                  e.printStackTrace();
                  fail();
                }
              }
            });
        threads.add(reader);
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      double endTime = System.currentTimeMillis();
      endTime = System.currentTimeMillis();
      double fTime = (endTime - startTime) / (double) 1000;
      System.out.println("test testConcurrency done itr " + itr + " : " + fTime + "s");
    }
  }

  @Test
  public void testConcurrencyRand() throws Exception {
    int itrs = 1;
    for (int itr = 1; itr <= itrs; itr++) {
      setUp();
      double startTime = (double) System.currentTimeMillis();

      int writersCnt = 200;
      int readersCnt = 200;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * i;
        Thread writer = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  btree.insert(
                      makeCompositekey(key, keyType), makeCompositekey(key, valueType));
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
        Thread reader = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  Compositekey result = btree.get(makeCompositekey(key, keyType));
                  assertEquals(
                      "Search operation failed",
                      0,
                      result.compareTo(makeCompositekey(key, valueType)));
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
      System.out.println("test testConcurrencyRand done itr " + itr + " : " + fTime + "s");
    }
  }

  // Delete Tests X(

  @Test
  public void testDeleteBasic() throws Exception {
    int itr = 1_000_000;
    double startTime = (double) System.currentTimeMillis();

    for (int i = 0; i < itr; i++) {
      try {
        btree.insert(makeCompositekey(i, keyType), makeCompositekey(i, valueType));
      } catch (Exception e) {
        System.out.println(i);
        e.printStackTrace();
        fail("Insert operation failed: " + e.getMessage());
      }
    }

    for (int start = 0; start < itr; start += 2000) {
      for (int i = start; i < start + 1000; i++) {
        try {
          boolean deleted = btree.delete(makeCompositekey(i, keyType));
          assertEquals(true, deleted);
        } catch (Exception e) {
          System.out.println(i);
          e.printStackTrace();
          fail("Insert operation failed: " + e.getMessage());
        }
      }
    }

    for (int start = 1000; start < itr; start += 2000) {
      for (int i = start; i < start + 1000; i++) {
        try {
          Compositekey result = btree.get(makeCompositekey(i, keyType));
          assertNotNull(result);
          assertEquals(
              "Search operation failed",
              0,
              result.compareTo(makeCompositekey(i, valueType)));
        } catch (Exception e) {
          System.out.println("expected -> " + i + "\n");
          e.printStackTrace();
          fail();
        }
      }
    }

    try {
      Cursor cursor = btree.begin();
      for (int start = 1000; start < itr && !cursor.isEnd(); start += 2000) {
        for (int i = start; i < start + 1000; i++, cursor.next()) {
          Cursor.Pair<Compositekey, Compositekey> curr = cursor.get();
          assertEquals(
              "Search operation failed", 0, curr.first.compareTo(makeCompositekey(i, keyType)));
          assertEquals(
              "Search operation failed", 0, curr.second.compareTo(makeCompositekey(i, valueType)));
        }
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
  public void testConcurrencyAllOut() throws Exception {
    int itrs = 5;
    for (int itr = 1; itr <= itrs; itr++) {
      setUp();
      double startTime = (double) System.currentTimeMillis();

      int writersCnt = 200;
      int DeleterCnt = 100;
      int readersCnt = 0;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  btree.insert(
                      makeCompositekey(key, keyType), makeCompositekey(key, valueType));
                } catch (Exception e) {
                  e.printStackTrace();
                  fail();
                }
              }
            });
        threads.add(writer);
      }

      for (int i = 0; i < DeleterCnt; i+=2) {
        final int end = op * i;
        Thread writer = new Thread(
            () -> {
              for (int key = end - op; key < end; key++) {
                try {
                  btree.delete(makeCompositekey(key, keyType));
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
      double endTime = System.currentTimeMillis();
      endTime = System.currentTimeMillis();
      double fTime = (endTime - startTime) / (double) 1000;
      System.out.println("test testConcurrencyRand done itr " + itr + " : " + fTime + "s");
    }
  }
}
