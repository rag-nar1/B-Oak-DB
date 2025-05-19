package test.btree;

import static org.junit.Assert.fail;

import btree.Btree;
import bufferpool.BufferPool;
import diskmanager.BasicDiskManager;
import diskmanager.DiskManager;
import globals.Globals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import types.Compositekey;
import types.Template;

public class BtreeBenchmark {
  private Btree btree;
  private BufferPool bufferPool;
  private DiskManager diskManager;
  private static final int MAX_PAGES = 4000; // Example max pages
  private static final int K = 30;
  private static final String btreeFilePath = "test.btree"; // Example file path for the B-tree
  private static final int ITERATIONS = 1;
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
  public void testBtreeBenchmark1() throws Exception {
    int keysnumber = 2_000_000;
    double[] totalExecutionTimes = new double[ITERATIONS];
    double[] totalInsertTimes = new double[ITERATIONS];
    double[] avgInsertTimes = new double[ITERATIONS];
    double[] insertThroughputs = new double[ITERATIONS];
    double[] totalSearchTimes = new double[ITERATIONS];
    double[] avgSearchTimes = new double[ITERATIONS];
    double[] searchThroughputs = new double[ITERATIONS];
    double[] overallThroughputs = new double[ITERATIONS];

    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
      setUp(); // Reset the environment for each iteration
      int writersCnt = 200;
      int readersCnt = 200;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;


      double[] insertTimes = new double[2 * keysnumber];
      double[] searchTimes = new double[keysnumber];

      threads = new ArrayList<>();
      double startTime = (double) System.currentTimeMillis();
      double startTimeW = (double) System.currentTimeMillis();
      // Insert operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long insertStartTime = System.currentTimeMillis();
                      btree.insert(
                          makeCompositekey(key, keysnumber - key, keyType),
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
      // delete operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long deleteStartTime = System.currentTimeMillis();
                      btree.delete(makeCompositekey(key, keysnumber - key, keyType));
                      long deleteEndTime = System.currentTimeMillis();
                      insertTimes[keysnumber + key] = (deleteEndTime - deleteStartTime);
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

      double insertTotalTime = System.currentTimeMillis() - startTimeW;
      totalInsertTimes[iteration] = insertTotalTime;
      avgInsertTimes[iteration] = Arrays.stream(insertTimes).average().getAsDouble();
      insertThroughputs[iteration] = 2 * keysnumber / insertTotalTime * 1000;

      // Search operations
      threads = new ArrayList<>();
      double startTime2 = System.currentTimeMillis();

      for (int i = 0; i < readersCnt; i++) {
        final int end = op * (i + 1);
        Thread reader =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long searchStartTime = System.currentTimeMillis();
                      Compositekey val =
                          btree.get(makeCompositekey(key, keysnumber - key, keyType));
                      long searchEndTime = System.currentTimeMillis();
                      searchTimes[key] = (searchEndTime - searchStartTime);
                      // assertEquals(0, val.compareTo(makeCompositekey(key, valueType)));
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

      double searchTotalTime = System.currentTimeMillis() - startTime2;
      totalSearchTimes[iteration] = searchTotalTime;
      avgSearchTimes[iteration] = Arrays.stream(searchTimes).average().getAsDouble();
      searchThroughputs[iteration] = keysnumber / searchTotalTime * 1000;

      double endTime = System.currentTimeMillis();
      totalExecutionTimes[iteration] = (endTime - startTime) / 1000.0;
      overallThroughputs[iteration] = (keysnumber * 3) / (insertTotalTime + searchTotalTime) * 1000;

      CleanUp(); // Clean up after each iteration
      System.out.println("Iteration " + (iteration + 1) + " completed");
    }

    // Calculate and display averages
    System.out.println("┌───────────────────────────────────────────────────────────");
    System.out.printf(
        "\033[1;36m              B-TREE BENCHMARK RESULTS (%d ITERATIONS)           \033[0m\n",
        ITERATIONS);
    System.out.println("┬───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1mAverage total execution time:\033[0m %8.2f seconds\n",
        Arrays.stream(totalExecutionTimes).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(" \033[1;32mInsert+Delete operations:\033[0m\n");
    System.out.printf(
        "   - Average total time:     %8.2f seconds\n",
        Arrays.stream(totalInsertTimes).average().getAsDouble() / 1000);
    System.out.printf(
        "   - Average operation time: %8.2f ms per operation\n",
        Arrays.stream(avgInsertTimes).average().getAsDouble());
    System.out.printf(
        "   - Average throughput:     %8.2f operations per second\n",
        Arrays.stream(insertThroughputs).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(" \033[1;33mSearch operations:\033[0m\n");
    System.out.printf(
        "   - Average total time:     %8.2f seconds\n",
        Arrays.stream(totalSearchTimes).average().getAsDouble() / 1000);
    System.out.printf(
        "   - Average operation time: %8.2f ms per operation\n",
        Arrays.stream(avgSearchTimes).average().getAsDouble());
    System.out.printf(
        "   - Average throughput:     %8.2f operations per second\n",
        Arrays.stream(searchThroughputs).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1;35mAverage overall throughput:\033[0m %8.2f operations per second\n",
        Arrays.stream(overallThroughputs).average().getAsDouble());
    System.out.println("└───────────────────────────────────────────────────────────");
  }

  @Test
  public void testBtreeBenchmark2() throws Exception {
    int keysnumber = 2_000_000;
    double[] totalExecutionTimes = new double[ITERATIONS];
    double[] totalInsertTimes = new double[ITERATIONS];
    double[] avgInsertTimes = new double[ITERATIONS];
    double[] insertThroughputs = new double[ITERATIONS];
    double[] totalSearchTimes = new double[ITERATIONS];
    double[] avgSearchTimes = new double[ITERATIONS];
    double[] searchThroughputs = new double[ITERATIONS];
    double[] overallThroughputs = new double[ITERATIONS];

    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
      setUp(); // Reset the environment for each iteration
      int writersCnt = 200;
      int readersCnt = 200;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;

      // Insert operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      // long insertStartTime = System.currentTimeMillis();
                      btree.insert(
                          makeCompositekey(key, keysnumber - key, keyType),
                          makeCompositekey(key, valueType));
                      // long insertEndTime = System.currentTimeMillis();
                      // insertTimes[key] = (insertEndTime - insertStartTime);
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

      double[] insertTimes = new double[2 * keysnumber];
      double[] searchTimes = new double[keysnumber];

      threads = new ArrayList<>();
      double startTime = (double) System.currentTimeMillis();
      double startTimeW = (double) System.currentTimeMillis();
      // Insert operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long insertStartTime = System.currentTimeMillis();
                      btree.insert(
                          makeCompositekey(key, keysnumber - key, keyType),
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
      // delete operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long deleteStartTime = System.currentTimeMillis();
                      btree.delete(makeCompositekey(key, keysnumber - key, keyType));
                      long deleteEndTime = System.currentTimeMillis();
                      insertTimes[keysnumber + key] = (deleteEndTime - deleteStartTime);
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

      double insertTotalTime = System.currentTimeMillis() - startTimeW;
      totalInsertTimes[iteration] = insertTotalTime;
      avgInsertTimes[iteration] = Arrays.stream(insertTimes).average().getAsDouble();
      insertThroughputs[iteration] = 2 * keysnumber / insertTotalTime * 1000;

      // Search operations
      threads = new ArrayList<>();
      double startTime2 = System.currentTimeMillis();

      for (int i = 0; i < readersCnt; i++) {
        final int end = op * (i + 1);
        Thread reader =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      long searchStartTime = System.currentTimeMillis();
                      Compositekey val =
                          btree.get(makeCompositekey(key, keysnumber - key, keyType));
                      long searchEndTime = System.currentTimeMillis();
                      searchTimes[key] = (searchEndTime - searchStartTime);
                      // assertEquals(0, val.compareTo(makeCompositekey(key, valueType)));
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

      double searchTotalTime = System.currentTimeMillis() - startTime2;
      totalSearchTimes[iteration] = searchTotalTime;
      avgSearchTimes[iteration] = Arrays.stream(searchTimes).average().getAsDouble();
      searchThroughputs[iteration] = keysnumber / searchTotalTime * 1000;

      double endTime = System.currentTimeMillis();
      totalExecutionTimes[iteration] = (endTime - startTime) / 1000.0;
      overallThroughputs[iteration] = (keysnumber * 3) / (insertTotalTime + searchTotalTime) * 1000;

      CleanUp(); // Clean up after each iteration
      System.out.println("Iteration " + (iteration + 1) + " completed");
    }

    // Calculate and display averages
    System.out.println("┌───────────────────────────────────────────────────────────");
    System.out.printf(
        "\033[1;36m              B-TREE BENCHMARK RESULTS (%d ITERATIONS)           \033[0m\n",
        ITERATIONS);
    System.out.println("┬───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1mAverage total execution time:\033[0m %8.2f seconds\n",
        Arrays.stream(totalExecutionTimes).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(" \033[1;32mInsert+Delete operations:\033[0m\n");
    System.out.printf(
        "   - Average total time:     %8.2f seconds\n",
        Arrays.stream(totalInsertTimes).average().getAsDouble() / 1000);
    System.out.printf(
        "   - Average operation time: %8.2f ms per operation\n",
        Arrays.stream(avgInsertTimes).average().getAsDouble());
    System.out.printf(
        "   - Average throughput:     %8.2f operations per second\n",
        Arrays.stream(insertThroughputs).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(" \033[1;33mSearch operations:\033[0m\n");
    System.out.printf(
        "   - Average total time:     %8.2f seconds\n",
        Arrays.stream(totalSearchTimes).average().getAsDouble() / 1000);
    System.out.printf(
        "   - Average operation time: %8.2f ms per operation\n",
        Arrays.stream(avgSearchTimes).average().getAsDouble());
    System.out.printf(
        "   - Average throughput:     %8.2f operations per second\n",
        Arrays.stream(searchThroughputs).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1;35mAverage overall throughput:\033[0m %8.2f operations per second\n",
        Arrays.stream(overallThroughputs).average().getAsDouble());
    System.out.println("└───────────────────────────────────────────────────────────");
  }

  @Test
  public void testBtreeBenchmark3() throws Exception {
    int keysnumber = 2_000_000;
    double[] totalExecutionTimes = new double[ITERATIONS];
    double[] overallThroughputs = new double[ITERATIONS];

    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
      setUp(); // Reset the environment for each iteration
      int writersCnt = 200;
      int readersCnt = 200;
      List<Thread> threads = new ArrayList<>();
      int op = 10000;

      // threads = new ArrayList<>();
      double startTime = (double) System.currentTimeMillis();
      double startTimeW = (double) System.currentTimeMillis();
      // Insert operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      // long insertStartTime = System.currentTimeMillis();
                      btree.insert(
                          makeCompositekey(key, keysnumber - key, keyType),
                          makeCompositekey(key, valueType));
                      // long insertEndTime = System.currentTimeMillis();
                    } catch (Exception e) {
                      e.printStackTrace();
                      fail();
                    }
                  }
                });
        threads.add(writer);
      }
      // delete operations
      for (int i = 0; i < writersCnt; i++) {
        final int end = op * (i + 1);
        Thread writer =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      // long deleteStartTime = System.currentTimeMillis();
                      btree.delete(makeCompositekey(key, keysnumber - key, keyType));
                      // long deleteEndTime = System.currentTimeMillis();
                      // insertTimes[keysnumber + key] = (deleteEndTime - deleteStartTime);
                    } catch (Exception e) {
                      e.printStackTrace();
                      fail();
                    }
                  }
                });
        threads.add(writer);
      }

      // Search operations
      // threads = new ArrayList<>();
      double startTime2 = System.currentTimeMillis();

      for (int i = 0; i < readersCnt; i++) {
        final int end = op * (i + 1);
        Thread reader =
            new Thread(
                () -> {
                  for (int key = end - op; key < end; key++) {
                    try {
                      // long searchStartTime = System.currentTimeMillis();
                      Compositekey val =
                          btree.get(makeCompositekey(key, keysnumber - key, keyType));
                      // long searchEndTime = System.currentTimeMillis();
                      // searchTimes[key] = (searchEndTime - searchStartTime);
                      // assertEquals(0, val.compareTo(makeCompositekey(key, valueType)));
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
      totalExecutionTimes[iteration] = (endTime - startTime) / 1000.0;
      overallThroughputs[iteration] = (keysnumber * 3) / (endTime - startTime) * 1000;

      CleanUp(); // Clean up after each iteration
      System.out.println("Iteration " + (iteration + 1) + " completed");
    }

    // Calculate and display averages
    System.out.println("┌───────────────────────────────────────────────────────────");
    System.out.printf(
        "\033[1;36m              B-TREE BENCHMARK RESULTS (%d ITERATIONS)           \033[0m\n",
        ITERATIONS);
    System.out.println("┬───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1mAverage total execution time:\033[0m %8.2f seconds\n",
        Arrays.stream(totalExecutionTimes).average().getAsDouble());
    System.out.println("┼───────────────────────────────────────────────────────────");
    System.out.printf(
        " \033[1;35mAverage overall throughput:\033[0m %8.2f operations per second\n",
        Arrays.stream(overallThroughputs).average().getAsDouble());
    System.out.println("└───────────────────────────────────────────────────────────");
  }
}
