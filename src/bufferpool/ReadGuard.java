package bufferpool;

import java.util.concurrent.locks.Lock;

public class ReadGuard extends Guard {
    
    public ReadGuard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        super(frameId, frame, replacer, bpmLatch);
        frame.lockRead();
        bpmLatch.lock();
        replacer.recordAccess(frameId);
        replacer.setEvictable(frameId, false);
        bpmLatch.unlock();
    }
}
