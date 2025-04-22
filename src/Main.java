
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

        // check if the data is written to the frame correctly
        System.out.println("Data in the frame: ");
        buffer.position(leafNode.getHeaderSize()); // Set position to the start of the data
        for(int i = 0; i < leafNode.getMaxKeysN(); i++) {
            int key = buffer.getInt(); // Read the key
            System.out.println("Key: " + key);
        }
        System.out.println("Data in the frame: ");
        buffer.position(leafNode.getHeaderSize() + leafNode.getMaxKeysN() * Integer.BYTES); // Set position to the start of the values  

        for(int i = 0; i < leafNode.getMaxKeysN(); i++) {
            int value = buffer.getInt(); // Read the value
            System.out.println("Value: " + value);
        }
        leafNode.writeHeader(); // Write the header to the frame
        // Write the frame to a file
        try (FileOutputStream fos = new FileOutputStream(new File(fileString))) {
            fos.write(frame.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read the frame from the file
        try (FileInputStream fis = new FileInputStream(new File(fileString))) {
            fis.read(frame.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a new LeafNode from the read data
        LeafNode<Integer, Integer> newLeafNode = new LeafNode<>(
                                            Integer.class, Integer.class, frame.getData());

        System.out.println("Number of keys in the new leaf node: " + newLeafNode.getKeysN());
        System.out.println("Max keys in the new node: " + newLeafNode.getMaxKeysN());
        System.out.println("Keys in the new leaf node: ");
        for (int i = 0; i < newLeafNode.getKeysN(); i++) {
            System.out.println("Key: " + newLeafNode.getKey(i) + ", Value: " + newLeafNode.getValue(i));
        }

    }
}
