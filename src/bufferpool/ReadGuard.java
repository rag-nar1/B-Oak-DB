package bufferpool;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

import globals.Globals;

public class ReadGuard extends Guard implements Closeable{
    
    public ReadGuard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        super(frameId, frame, replacer, bpmLatch);
         try {
            boolean locked = frame.lockRead();
            if(!locked) {
                this.frameId = Globals.INVALID_Frame_ID;
            }
        } catch (Exception e) {
            this.frameId = Globals.INVALID_Frame_ID;
        }
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
