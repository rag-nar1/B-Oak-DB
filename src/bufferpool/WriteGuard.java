package bufferpool;

import globals.Globals;
import java.util.concurrent.locks.Lock;

public class WriteGuard extends Guard {
  public WriteGuard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
    super(frameId, frame, replacer, bpmLatch);
    try {
      boolean locked = frame.lockWrite();
      if (!locked) {
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
    frame.unlockWrite();
  }

  public byte[] getDataMut() {
    frame.setDirty(true);
    return frame.getData();
  }
}
