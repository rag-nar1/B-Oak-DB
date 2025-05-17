package test.diskmanager;

import static org.junit.Assert.*;

import diskmanager.BasicDiskManager;
import diskmanager.DiskManager;
import diskmanager.DiskRequest;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DiskManagerTest {
  private DiskManager diskManager;
  private static final String TEST_FILE = "test.db";
  private static final int PAGE_SIZE = 4096;

  @Before
  public void setUp() throws Exception {
    diskManager = new BasicDiskManager();
  }

  @After
  public void tearDown() throws Exception {
    if (diskManager.getFileCount() >= 0) {
      diskManager.close();
    }
    // Clean up test file if it exists
    java.io.File testFile = new java.io.File("storage/" + TEST_FILE);
    if (testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  public void testFileCreationAndCount() throws IOException {
    assertEquals(0, diskManager.getFileCount());
    diskManager.open(TEST_FILE);
    assertEquals(1, diskManager.getFileCount());

    // Opening same file should not increase count
    diskManager.open(TEST_FILE);
    assertEquals(1, diskManager.getFileCount());
  }

  @Test
  public void testPageAllocation() throws IOException {
    long pageId = diskManager.allocatePage(TEST_FILE);
    assertEquals(0, pageId); // First page should be 0

    long secondPageId = diskManager.allocatePage(TEST_FILE);
    assertEquals(1, secondPageId); // Second page should be 1
  }

  @Test
  public void testReadWriteRequests() throws Exception {
    // Allocate a page first
    long pageId = diskManager.allocatePage(TEST_FILE);

    // Prepare test data
    byte[] writeData = new byte[PAGE_SIZE];
    Arrays.fill(writeData, (byte) 42);

    // Write request
    DiskRequest writeRequest = new DiskRequest(TEST_FILE, pageId, writeData, true);
    diskManager.pushRequest(writeRequest);

    assertTrue(writeRequest.getFuture().get()); // Wait for write to complete

    // Read back the data
    byte[] readData = new byte[PAGE_SIZE];
    DiskRequest readRequest = new DiskRequest(TEST_FILE, pageId, readData, false);
    diskManager.pushRequest(readRequest);

    assertTrue(readRequest.getFuture().get()); // Wait for read to complete
    assertArrayEquals(writeData, readData);
  }

  @Test
  public void testConcurrentRequests() throws Exception {
    int numRequests = 10;
    DiskRequest[] requests = new DiskRequest[numRequests];
    long pageId = diskManager.allocatePage(TEST_FILE);

    // Launch multiple concurrent write requests
    for (int i = 0; i < numRequests; i++) {
      byte[] data = new byte[PAGE_SIZE];
      Arrays.fill(data, (byte) i);
      DiskRequest request = new DiskRequest(TEST_FILE, pageId, data, true);
      requests[i] = request;
      diskManager.pushRequest(request);
    }

    // Wait for all requests to complete
    for (DiskRequest request : requests) {
      assertTrue(request.getFuture().get());
    }
  }

  @Test(expected = NullPointerException.class)
  public void testOperationsAfterClose() throws IOException, NullPointerException {
    diskManager.close();
    // This should throw NullPointerException
    diskManager.open(TEST_FILE);
  }
}
