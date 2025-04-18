package bufferpool;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
/**
 * to access the data of a frame writing or reading in throw this
 */
public class Guard implements Closeable{

    private int frameId;
    private Frame frame;
    private Replacer replacer;
    private Lock bpmLatch;

    public Guard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        this.frameId = frameId; 
        this.frame = frame; 
        this.replacer = replacer; 
        this.bpmLatch = bpmLatch;

        frame.addPin();
        bpmLatch.lock();
        replacer.recordAccess(frameId);
        if (frame.getPinCount() == 1) {
            replacer.setEvictable(frameId, false);
        }
        bpmLatch.unlock();
    }

    public byte[] getDataMut() {
        frame.setDirty(true);
        return frame.getData();
    }

    public ByteBuffer getData() {
        return ByteBuffer.wrap(frame.getData()).asReadOnlyBuffer();
    }

    public void close() {
        int pinCount = frame.removePin();
        bpmLatch.lock();
        if (pinCount == 0) {
            replacer.setEvictable(frameId, true);
        }
        bpmLatch.unlock();
        frame.unlock();
    }
}

