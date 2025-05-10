package test.bufferpool;

import bufferpool.LRU;
import bufferpool.Replacer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReplacerTest {
    private Replacer replacer;
    private static final int K_DISTANCE = 5;

    @Before
    public void setUp() {
        replacer = new LRU(K_DISTANCE);
    }

    @Test
    public void testBasicEviction() {
        // Record accesses for two frames
        replacer.recordAccess(0);
        replacer.recordAccess(1);
        
        // Frame 0 should be evicted as it was accessed earlier
        assertEquals(0, replacer.evict());
    }

    @Test
    public void testNoEvictionWhenAllUnevictable() {
        // Record accesses and make frames unevictable
        replacer.recordAccess(0);
        replacer.recordAccess(1);
        replacer.setEvictable(0, false);
        replacer.setEvictable(1, false);
        
        // Should return -1 when no frames can be evicted
        assertEquals(-1, replacer.evict());
    }

    @Test
    public void testEvictionWithKDistance() {
        // Simulate K accesses for frame 0
        for (int i = 0; i < K_DISTANCE; i++) {
            replacer.recordAccess(0);
        }
        
        // Single access for frame 1
        replacer.recordAccess(1);
        
        // Frame 1 should be evicted as it has fewer accesses
        assertEquals(1, replacer.evict());
    }

    @Test
    public void testFrameDeletion() {
        // Record access and then delete frame
        replacer.recordAccess(0);
        replacer.deleteFrame(0);
        
        // After deletion, no frames should be available for eviction
        assertEquals(-1, replacer.evict());
    }

    @Test
    public void testEvictableStateTransitions() {
        replacer.recordAccess(0);
        
        // Make frame unevictable
        replacer.setEvictable(0, false);
        assertEquals(-1, replacer.evict());
        
        // Make frame evictable again
        replacer.setEvictable(0, true);
        assertEquals(0, replacer.evict());
    }

    @Test
    public void testTieBreakingWithInfiniteKDistance() {
        // Record single access for multiple frames
        replacer.recordAccess(0);
        replacer.recordAccess(1);
        replacer.recordAccess(2);
        
        // All frames have infinite k-distance (less than K accesses)
        // Should evict frame 0 as it has the earliest recent access
        assertEquals(0, replacer.evict());
    }
    
    @Test
    public void testConcurrentRecordAccess() throws InterruptedException {
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        
        // Create threads that will concurrently record access
        for (int i = 0; i < numThreads; i++) {
            final int frameId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    replacer.recordAccess(frameId);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify that the first frame has the most recent access and won't be evicted
        assertNotEquals(-1, replacer.evict());
    }

    @Test
    public void testConcurrentEviction() throws InterruptedException {
        // Setup initial state
        for (int i = 0; i < 5; i++) {
            replacer.recordAccess(i);
        }

        int numThreads = 3;
        Thread[] threads = new Thread[numThreads];
        final int[] evictedFrames = new int[numThreads];
        
        // Create threads that will try to evict frames concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                evictedFrames[threadIndex] = replacer.evict();
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify that each frame was evicted exactly once
        boolean[] evicted = new boolean[5];
        for (int frameId : evictedFrames) {
            if (frameId != -1) {
                assertFalse("Frame " + frameId + " was evicted multiple times", evicted[frameId]);
                evicted[frameId] = true;
            }
        }
    }

    @Test
    public void testConcurrentEvictableStateTransitions() throws InterruptedException {
        final int frameId = 0;
        replacer.recordAccess(frameId);
        
        Thread setterThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                replacer.recordAccess(frameId);
            }
        });
        
        Thread evictorThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                replacer.evict();
            }
        });
        
        setterThread.start();
        evictorThread.start();
        
        setterThread.join();
        evictorThread.join();
        
        // Final state should be consistent
        replacer.setEvictable(frameId, true);
        int evictedFrame = replacer.evict();
        assertTrue("Frame should either be evicted or not exist", evictedFrame == frameId || evictedFrame == -1);
    }

    @Test
    public void testConcurrentEvictionRecording() throws InterruptedException {
        // Setup initial state
        for (int i = 0; i < 5; i++) {
            replacer.recordAccess(i);
            replacer.recordAccess(i);
            replacer.recordAccess(i);
        }
        
        class Triple {
            int frameId;
            int time;
            char type;
            Triple(int frameId, int time, char type) {
                this.frameId = frameId;
                this.time = time;
                this.type = type;
            }
        }
        

        int numThreads = 10;
        Thread[] evicting = new Thread[numThreads];
        Thread[] recording = new Thread[numThreads];
        AtomicInteger currentTime = new AtomicInteger(0);
        List<Triple> timeLine = new ArrayList<>();
        Lock listLock = new ReentrantLock();
        // Create threads that will try to evict frames concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            evicting[i] = new Thread(() -> {
                int evictedFrames = replacer.evict();
                int time = currentTime.incrementAndGet();
                listLock.lock();
                timeLine.add(new Triple(evictedFrames, time, 'E'));
                listLock.unlock();
            });
        }
        Random random = new Random();
        // Create threads that will record access concurrently
        for (int i = 0; i < numThreads; i++) {
            recording[i] = new Thread(() -> {
                int frameId = random.nextInt(5);
                replacer.recordAccess(frameId);
                int time = currentTime.incrementAndGet();
                listLock.lock();
                timeLine.add(new Triple(frameId, time, 'R'));
                listLock.unlock();
            });
        }
        
        // Start all threads
        for (Thread thread : recording) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : evicting) {
            thread.join();
        }

        timeLine.sort((a, b) -> Integer.compare(a.time, b.time));
        // Verify that each frame was evicted exactly once
        char[] state = new char[5];
        for (Triple t : timeLine) {
            assertFalse(state[t.frameId] == 'E' && t.type == 'E');
            state[t.frameId] = t.type;
        }
        
    }
}