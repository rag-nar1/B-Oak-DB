package bufferpool;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public class ReadGuard extends Guard implements Closeable{
    
    public ReadGuard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        super(frameId, frame, replacer, bpmLatch);
        frame.lockRead();
    }
    
    public void close() {
        bpmLatch.lock();
        int pinCount = frame.removePin();
        if (pinCount == 0) {
            replacer.setEvictable(frameId, true);
        }
        bpmLatch.unlock();
        frame.unlockRead();
    }
}
