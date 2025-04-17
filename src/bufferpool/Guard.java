package bufferpool;
import java.util.concurrent.locks.Lock;
/**
 * to access the data of a frame writing or reading in throw this
 */
public class Guard {

    private int frameId;
    private Frame frame;
    private Replacer replacer;
    private Lock bpmLatch;

    public Guard(int frameId, Frame frame, Replacer replacer, Lock bpmLatch) {
        this.frameId = frameId; 
        this.frame = frame; 
        this.replacer = replacer; 
        this.bpmLatch = bpmLatch;
    }

    
}

