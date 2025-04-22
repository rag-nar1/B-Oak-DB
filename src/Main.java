
import page.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import bufferpool.Frame;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException{
        String fileString = "storage/test.db";
        long pageId = 1; // Example page ID
        // Frame frame = new Frame(1); // Example frame size
        Frame frame = new Frame(1); // Example frame size
        // set the stage for the node
        ByteBuffer buffer = ByteBuffer.wrap(frame.getData());
        buffer.putShort((short) 0); // Number of keys (initially 0)
        buffer.put((byte) 1); // isLeaf (1 for leaf node)
        buffer.putLong(pageId); // pageId
        buffer.putLong(-1); // nextLeafNode (initially -1)

        LeafNode<Integer, Integer> leafNode = new LeafNode<>(
                                            Integer.class, Integer.class, frame.getData());
        
        System.out.println("Number of keys in the leaf node: " + leafNode.getKeysN());
        System.out.println("Max keys in the node: " + leafNode.getMaxKeysN());
        
        for(int i = 0; i < leafNode.getMaxKeysN(); i++) {
            leafNode.setKey(i, i + 1); // Set keys from 1 to maxKeysN
            leafNode.setValue(i, (i + 1) * 10); // Set values as multiples of 10
        }

        leafNode.setKeysN((short) leafNode.getMaxKeysN()); // Set the number of keys
        System.out.println("Keys in the leaf node: ");
        for (int i = 0; i < leafNode.getKeysN(); i++) {
            System.out.println("Key: " + leafNode.getKey(i) + ", Value: " + leafNode.getValue(i));
        }
    }
}
