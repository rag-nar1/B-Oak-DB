package bufferpool;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * implementation of the LRU-K replacer
 */
public class Replacer {
    
    private class FrameMeta {
        private int frameId;
        private boolean evictable;
        private List<Long> timeStamps;
        
        public FrameMeta(int frameId) {
            this.frameId = frameId;
            timeStamps = new LinkedList<Long>();
            evictable = true;
        }

        public void recordAccess(long time, int k) {
            if (timeStamps.size() == k) {
                timeStamps.removeLast();
            }
            timeStamps.addFirst(time);
        }

        public long kDistance(long current, int k) {
            if (timeStamps.size() < k) {
                return Long.MAX_VALUE;
            }

            return current - timeStamps.getLast();
        }

        public long recentAccess() {
            return timeStamps.getFirst();
        }

        public void setEvictable(boolean evictable) {
            this.evictable = evictable;
        }

        public boolean isEvictable() {
            return evictable;
        }
    }

    private int k;
    private long currentTime;
    private Map<Integer, FrameMeta> frames;
    private Lock latch;

    public Replacer(int k) {
        this.k = k;
        currentTime = 0;
        frames = new HashMap<Integer, FrameMeta>();
        latch = new ReentrantLock();
    }

    /**
     * record access at the currentTime to the frame
     * @param frameId
     */
    public void recordAccess(int frameId) {
        latch.lock();
        if (!frames.containsKey(frameId)) {
            frames.put(frameId, new FrameMeta(frameId));
        }
        FrameMeta frame = frames.get(frameId);
        frame.recordAccess(currentTime, k);
        currentTime ++;
        latch.unlock();
    }

    /**
     * evict the frame with the largest k-distance 
     * in case if two frames tie with the INF 
     * we break the tie with who has the latest access and evict the other
     * also we skip any unevictable frames
     * @return the frame to be evicted
     */
    public int evict() {
        latch.lock();
        int victim = -1;
        long victimRecentAccess = -1;
        long max = -1;
        for(Map.Entry<Integer, FrameMeta> current: frames.entrySet()) {
            int frameId = current.getKey();
            FrameMeta frame = current.getValue();
            if (!frame.isEvictable()) {
                continue;
            }

            long kDistance = frame.kDistance(currentTime, k);
            if (kDistance > max) {
                victim = frameId;
                max = kDistance;
                victimRecentAccess = frame.recentAccess();
            } else if (kDistance == Long.MAX_VALUE && max == kDistance) {
                long recentAccess = frame.recentAccess();
                if (recentAccess < victimRecentAccess) {
                    victim = frameId;
                    victimRecentAccess = recentAccess;
                }
            }
        }

        if (victim == -1) {
            return -1;
        }

        frames.remove(victim);
        latch.unlock();

        return victim;
    }

    /**
     * set a frame to a state [evictable -> true, unevictable -> false] 
     * @param frameId
     * @param evictable
     */
    public void setEvictable(int frameId, boolean evictable) {
        latch.lock();
        if (!frames.containsKey(frameId)) {
            latch.unlock();
            return;
        }

        FrameMeta frame = frames.get(frameId);
        frame.setEvictable(evictable);
        latch.unlock();
    }
}
