package test.bufferpool;

import bufferpool.*;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class GuardTest {
    private Frame frame;
    private Replacer replacer;
    private ReentrantLock bpmLatch;
    private static final int FRAME_ID = 1;
    private static final int PAGE_ID = 2;
    private static final String FILE_NAME = "test.db";
    private static final int K_DISTANCE = 3;

    @Before
    public void setUp() {
        frame = new Frame(FRAME_ID, PAGE_ID, FILE_NAME);
        replacer = new Replacer(K_DISTANCE);
        bpmLatch = new ReentrantLock();
    }

    @Test
    public void testBasicGuardCreation() {
        ReadGuard readGuard = new ReadGuard(FRAME_ID, frame, replacer, bpmLatch);
        assertEquals(1, frame.getPinCount());
        readGuard.close();
        assertEquals(0, frame.getPinCount());

        WriteGuard writeGuard = new WriteGuard(FRAME_ID, frame, replacer, bpmLatch);
        assertEquals(1, frame.getPinCount());
        writeGuard.close();
        assertEquals(0, frame.getPinCount());
    }

    @Test
    public void testReadWriteGuardDataAccess() {
        WriteGuard writeGuard = new WriteGuard(FRAME_ID, frame, replacer, bpmLatch);
        byte[] data = writeGuard.getDataMut();
        data[0] = 42;
        assertTrue(frame.isDirty());
        writeGuard.close();

        ReadGuard readGuard = new ReadGuard(FRAME_ID, frame, replacer, bpmLatch);
        assertEquals(42, readGuard.getData().get(0));
        readGuard.close();
    }

    @Test
    public void testMultipleReadGuards() throws InterruptedException {
        int numReaders = 5;
        CyclicBarrier barrier = new CyclicBarrier(numReaders);
        CountDownLatch doneLatch = new CountDownLatch(numReaders);
        AtomicInteger activeReaders = new AtomicInteger(0);
        AtomicBoolean error = new AtomicBoolean(false);

        Thread[] readers = new Thread[numReaders];
        for (int i = 0; i < numReaders; i++) {
            readers[i] = new Thread(() -> {
                try {
                    barrier.await();
                    ReadGuard guard = new ReadGuard(FRAME_ID, frame, replacer, bpmLatch);
                    int readerscount = activeReaders.incrementAndGet();
                    if (readerscount < 1) {
                        error.set(true);
                    }
                    Thread.sleep(10);
                    activeReaders.decrementAndGet();
                    guard.close();
                } catch (Exception e) {
                    error.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
            readers[i].start();
        }

        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
        assertFalse("Concurrent read guard error occurred", error.get());
        assertEquals(0, frame.getPinCount());
    }

    @Test
    public void testWriteGuardExclusion() throws InterruptedException {
        CountDownLatch writerStarted = new CountDownLatch(1);
        CountDownLatch readerStarted = new CountDownLatch(1);
        AtomicBoolean error = new AtomicBoolean(false);

        // Start writer thread
        Thread writer = new Thread(() -> {
            WriteGuard guard = new WriteGuard(FRAME_ID, frame, replacer, bpmLatch);
            writerStarted.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                guard.close();
            }
        });

        // Start reader thread
        Thread reader = new Thread(() -> {
            try {
                writerStarted.await();
                ReadGuard guard = new ReadGuard(FRAME_ID, frame, replacer, bpmLatch);
                error.set(true); // Should not reach here while writer holds lock
                readerStarted.countDown();
                guard.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        writer.start();
        reader.start();
        
        // Wait for a short time to ensure reader is blocked
        assertFalse("Reader should be blocked by writer", readerStarted.await(50, TimeUnit.MILLISECONDS));
        assertFalse("Reader should not have acquired lock while writer active", error.get());
    }

    @Test
    public void testEvictionStateManagement() {
        WriteGuard guard = new WriteGuard(FRAME_ID, frame, replacer, bpmLatch);
        
        // Frame should not be evictable while guard is active
        assertEquals(-1, replacer.evict());
        
        // Frame should become evictable after guard is closed
        guard.close();
        assertEquals(FRAME_ID, replacer.evict());
    }

    @Test
    public void testConcurrentGuardCreation() throws InterruptedException {
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2 * numThreads);
        AtomicInteger totalPinCount = new AtomicInteger(0);

        Thread[] threads = new Thread[2 * numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    Guard guard = new ReadGuard(FRAME_ID, frame, replacer, bpmLatch);
                    totalPinCount.incrementAndGet();
                    Thread.sleep(10);
                    guard.close();
                    totalPinCount.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads[i].start();

            threads[i + numThreads] = new Thread(() -> {
                try {
                    startLatch.await();
                    Guard guard = new WriteGuard(FRAME_ID, frame, replacer, bpmLatch);
                    totalPinCount.incrementAndGet();
                    Thread.sleep(10);
                    guard.close();
                    totalPinCount.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads[i + numThreads].start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
        assertEquals(0, totalPinCount.get());
        assertEquals(0, frame.getPinCount());
    }
}