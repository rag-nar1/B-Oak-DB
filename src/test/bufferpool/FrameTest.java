package test.bufferpool;

import bufferpool.Frame;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameTest {
    private Frame frame;
    private static final int FRAME_ID = 1;
    private static final int PAGE_ID = 2;
    private static final String FILE_NAME = "test.db";
    private static final int PAGE_SIZE = 4096;

    @Before
    public void setUp() {
        frame = new Frame(FRAME_ID, PAGE_ID, FILE_NAME);
    }

    @Test
    public void testConstructor() {
        assertEquals(FRAME_ID, frame.getFrameId());
        assertEquals(PAGE_ID, frame.getPageId());
        assertEquals(FILE_NAME, frame.getFileName());
        assertFalse(frame.isDirty());
        assertEquals(0, frame.getPinCount());
        assertEquals(PAGE_SIZE, frame.getData().length);
    }

    @Test
    public void testNewFrame() {
        long newPageId = 3;
        String newFileName = "new.db";
        frame.newFrame(newPageId, newFileName);
        
        assertEquals(newPageId, frame.getPageId());
        assertEquals(newFileName, frame.getFileName());
        assertEquals(0, frame.getPinCount());
    }

    @Test
    public void testPinCount() {
        assertEquals(0, frame.getPinCount());
        assertEquals(1, frame.addPin());
        assertEquals(2, frame.addPin());
        assertEquals(1, frame.removePin());
        assertEquals(0, frame.removePin());
    }

    @Test
    public void testDirtyFlag() {
        assertFalse(frame.isDirty());
        frame.setDirty(true);
        assertTrue(frame.isDirty());
        frame.setDirty(false);
        assertFalse(frame.isDirty());
    }

    @Test
    public void testDataAccess() {
        byte[] data = frame.getData();
        assertEquals(PAGE_SIZE, data.length);
        
        // Modify data
        data[0] = 42;
        assertArrayEquals(data, frame.getData());
    }

    @Test
    public void testConcurrentPinCount() throws InterruptedException {
        int numThreads = 10;
        int pinsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < pinsPerThread; j++) {
                        frame.addPin();
                        frame.removePin();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            threads[i].start();
        }
        
        startLatch.countDown();
        doneLatch.await();
        
        assertEquals(0, frame.getPinCount());
    }

    @Test
    public void testConcurrentReadLocks() throws InterruptedException {
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
                    frame.lockRead();
                    int currentReaders = activeReaders.incrementAndGet();
                    // Multiple readers should be able to read simultaneously
                    if (currentReaders < 1) {
                        error.set(true);
                    }
                    Thread.sleep(10);
                    activeReaders.decrementAndGet();
                    frame.unlockRead();
                } catch (Exception e) {
                    error.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
            readers[i].start();
        }
        
        doneLatch.await();
        assertFalse("Concurrent read lock error occurred", error.get());
    }

    @Test
    public void testReadWriteLockExclusion() throws InterruptedException {
        CountDownLatch writerStarted = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);
        AtomicBoolean error = new AtomicBoolean(false);
        
        // Start writer thread
        Thread writer = new Thread(() -> {
           try{
                frame.lockWrite();
                writerStarted.countDown();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    frame.unlockWrite();
                }
            } catch (Exception e) {
                fail();
            }
        });
        
        // Start reader thread
        Thread reader = new Thread(() -> {
            try {
                writerStarted.await();
                frame.lockRead();
                frame.unlockRead();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readerDone.countDown();
            }
        });
        
        writer.start();
        reader.start();
        
        readerDone.await();
        assertFalse("Reader acquired lock while writer was active", error.get());
    }
}