package page;

import java.io.Serializable;
import java.nio.ByteBuffer;

import globals.Globals;
import types.GenericByteReader;
import types.Types;

/**
 * LeafNode class represents a leaf node in a B+ tree.
 * It extends the TreeNode class and is used to store keys and values.
 * The keys are used to get the corresponding values (document in the primary index, primary key in the secondary index).
 * The leaf node is the last level of the B+ tree, where the actual data is stored in the cluster index.
 * The leaf node contains a pointer to the next leaf node, which is used for sequential access.
 * The last pageId points to the next leaf node.
 | key1   |key2   |key3   |...|keyN   |
 |value1 |value2 |value3 |...|valueN |
 
    the header is
    * 2 bytes for the number of keys
    * 1 byte for the type of the node -> 00000001 for leaf node , 00000000 for internal node
    * 8 bytes for the pageId
    * 8 bytes for the next leaf node
    * the rest is the keys and values
    * The keys are stored in the first part of the page, and the values are stored in the second part.
    * | 2bytes | 1byte | 8 bytes | 8 bytes | keys... | values... |
    *| KeysN  | Type  | pageId |nextLeafNode| key1    | key2    | ... | keyN    | value1 | value2 | ... | valueN | 
 */
public class LeafNode<KeyType extends Comparable<KeyType> & Serializable, ValueType extends Serializable> extends TreeNode<KeyType, ValueType> {
    private final Class<KeyType> keyType;
    private final Class<ValueType> valueType;
    // The next leaf node in the linked list
    private long nextLeafNode;
    private final short headerSize = 2 + 1 + 8 + 8; // 2 bytes for keysN, 2 bytes for keySize, 2 bytes for valueSize, 1 byte for type, 8 bytes for pageId, 8 bytes for nextLeafNode
    public LeafNode(Class<KeyType> keyType, Class<ValueType> valueType, long pageId) {
        super(pageId, true);
        this.keyType = keyType;
        this.valueType = valueType;
        keySize = Types.getSize(keyType);
        valueSize = Types.getSize(valueType);
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);
    }

    public LeafNode(Class<KeyType> keyType, Class<ValueType> valueType, byte[] rowData) {
        this.keyType = keyType;
        this.valueType = valueType;
        keySize = Types.getSize(keyType);
        valueSize = Types.getSize(valueType);
        ByteBuffer buffer = ByteBuffer.wrap(rowData);
        this.keysN = buffer.getShort();
        this.isLeaf = buffer.get() == 1;
        this.pageId = buffer.getLong();
        this.nextLeafNode = buffer.getLong();
        this.keys = (KeyType[]) new Comparable[keysN];
        this.values = (ValueType[]) new Object[keysN];
        for (int i = 0; i < keysN; i++) {
            // Assuming KeyType and ValueType have a method to read from ByteBuffer
            keys[i] = readKey(buffer);
            values[i] = readValue(buffer);
        }
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);
    }

    private KeyType readKey(ByteBuffer buffer) {
        // Implement the logic to read a key from the buffer
        // This is a placeholder implementation
        byte[] keyBytes = new byte[keySize];
        buffer.get(keyBytes);
        // Read the key from the buffer 
        try {
            return GenericByteReader.convert(keyBytes, GenericByteReader.primitiveConverter(keyType));
        } catch (Exception e) {
            throw new RuntimeException("Error reading key from buffer", e);
        }
    }

    private ValueType readValue(ByteBuffer buffer) {
        // Implement the logic to read a value from the buffer
        // This is a placeholder implementation
        byte[] valueBytes = new byte[valueSize];
        buffer.get(valueBytes);
        // Read the value from the buffer 
        try {
            return GenericByteReader.convert(valueBytes, GenericByteReader.primitiveConverter(valueType));
        } catch (Exception e) {
            throw new RuntimeException("Error reading value from buffer", e);
        }
    }

    public long getNextLeafNode() {
        return nextLeafNode;
    }

    public void setNextLeafNode(long nextLeafNode) {
        this.nextLeafNode = nextLeafNode;
    }
}

