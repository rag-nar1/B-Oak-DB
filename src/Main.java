
import page.*;
import java.io.Serializable;

import bufferpool.Frame;
import globals.Globals;
public class Main {
    public static void main(String[] args) {
        short maxKeysN = 4; // Example maximum number of keys
        long pageId = 1; // Example page ID
        Frame frame = new Frame(1); // Example frame size
        LeafNode<Integer, String> leafNode = new LeafNode<>(frame.getData());

    }
}
