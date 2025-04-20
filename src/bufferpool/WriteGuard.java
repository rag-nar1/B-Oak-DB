package bufferpool;

import java.util.concurrent.locks.Lock;

public class WriteGuard extends Guard {
    public WriteGuard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        super(frameId, frame, replacer, bpmLatch);
        frame.lockWrite();
    }

    public void close() {
        bpmLatch.lock();
        int pinCount = frame.removePin();
        if (pinCount == 0) {
            replacer.setEvictable(frameId, true);
        }
        bpmLatch.unlock();
        frame.unlockWrite();
    }

    public byte[] getDataMut() {
        frame.setDirty(true);
        return frame.getData();
    }

}
