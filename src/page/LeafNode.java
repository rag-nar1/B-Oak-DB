package page;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import bufferpool.BufferPool;
import bufferpool.WriteGuard;
import globals.Globals;
import types.Array;
import types.CompareableArray;
import types.Types;

/**
 * LeafNode class represents a leaf node in a B+ tree.
 * It extends the TreeNode class and is used to store keys and values.
 * The keys are used to get the corresponding values (document in the primary
 * index, primary key in the secondary index).
 * The leaf node is the last level of the B+ tree, where the actual data is
 * stored in the cluster index.
 * The leaf node contains a pointer to the next leaf node, which is used for
 * sequential access.
 * The last pageId points to the next leaf node.
 * | key1 |key2 |key3 |...|keyN |
 * |value1 |value2 |value3 |...|valueN |
 * 
 * the header is
 * 2 bytes for the number of keys
 * 1 byte for the type of the node -> 00000001 for leaf node , 00000000 for
 * internal node
 * 8 bytes for the pageId
 * 8 bytes for the next leaf node
 * the rest is the keys and values
 * The keys are stored in the first part of the page, and the values are stored
 * in the second part.
 * | 2bytes | 1byte | 8 bytes | 8 bytes | keys... | values... |
 * | KeysN | Type | pageId |nextLeafNode| key1 | key2 | ... | keyN | value1 |
 * value2 | ... | valueN |
 */
public class LeafNode<KeyType extends Comparable<KeyType>, ValueType> extends TreeNodeHeader {
    // The next leaf node in the linked list
    private long nextLeafNode; // this is the next 8 bytes of the header
    private final short headerSize = 2 + 1 + 8 + 8; // 2 bytes for keysN, 2 bytes for keySize, 2 bytes for valueSize, 1
                                                    // byte for type, 8 bytes for pageId, 8 bytes for nextLeafNode
    private final Class<KeyType> keyType;
    private final Class<ValueType> valueType;
    private final short keySize;
    private final short valueSize;
    private final short maxKeysN;
    private final short minKeysN;
    private CompareableArray<KeyType> keys;
    private Array<ValueType> values;

    public LeafNode(Class<KeyType> keyType, Class<ValueType> valueType, long pageId) {
        super(pageId, true);
        this.keyType = keyType;
        this.valueType = valueType;
        keySize = Types.getSize(keyType);
        valueSize = Types.getSize(valueType);
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);
        nextLeafNode = Globals.INVALID_PAGE_ID;
    }

    public LeafNode(Class<KeyType> keyType, Class<ValueType> valueType, ByteBuffer rawData) {
        this.keyType = keyType;
        this.valueType = valueType;
        keySize = Types.getSize(keyType);
        valueSize = Types.getSize(valueType);
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);

        buffer = rawData;
        this.keysN = buffer.getShort();
        this.isLeaf = buffer.get() == 1;
        this.pageId = buffer.getLong();
        this.nextLeafNode = buffer.getLong();

        keys = new CompareableArray<KeyType>(rawData, Array.getCodec(keyType), headerSize, keysN, maxKeysN);
        values = new Array<>(rawData, Array.getCodec(valueType), headerSize + maxKeysN * keySize, keysN, maxKeysN);
    }

    public LeafNode(Class<KeyType> keyType, Class<ValueType> valueType, byte[] rawData) {
        this(keyType, valueType, ByteBuffer.wrap(rawData));
    }

    public void writeHeader() {
        buffer.rewind();
        buffer.putShort(keysN);
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putLong(pageId);
        buffer.putLong(nextLeafNode);
    }

    public boolean insert(KeyType key, ValueType value) {
        if (keysN >= maxKeysN) {
            return false; // node is full
        }
        
        int index = keys.upperBound(key);
        keys.insert(index, key);
        values.insert(index, value);
        keysN++;
        writeHeader();
        return true;
    }

    public ValueType get(KeyType key) {
        int index = keys.binarySearch(key);
        if (index == -1) {
            return null; // key not found
        } else {
            return values.get(index); // return the value
        }
    }

    public WriteGuard split(BufferPool bufferPool, String fileName) {
        if (keysN < maxKeysN) {
            return null; // node is not full
        }

        try {
            // create a new leaf node
            long newPageId = bufferPool.allocateNewPage(fileName);
            WriteGuard newGuard = bufferPool.getWriteGuard(fileName, newPageId);
            if(newGuard == null) {
               return null;
            }
            LeafNode<KeyType, ValueType> newLeafNode = new LeafNode<>(keyType, valueType, newGuard.getDataMut());
            newLeafNode.setLeaf(true);
            newLeafNode.setPageId(newPageId);
            // copy half of the keys and values to the new leaf node
            for (int i = minKeysN; i < keysN; i++) {
                newLeafNode.keys.set(i - minKeysN, keys.get(i));
                newLeafNode.values.set(i - minKeysN, values.get(i));
            }
            newLeafNode.setKeysN((short) (keysN - minKeysN));
            setKeysN(minKeysN);
            // set the next leaf node of the new leaf node
            newLeafNode.nextLeafNode = nextLeafNode;
            // set the next leaf node of the current leaf node
            nextLeafNode = newPageId;

            // write the new leaf node to the buffer pool
            newLeafNode.writeHeader();
            writeHeader();
            return newGuard;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters and Setters

    public long getNextLeafNode() {
        return nextLeafNode;
    }

    public short getHeaderSize() {
        return headerSize;
    }

    public Class<KeyType> getKeyType() {
        return keyType;
    }

    public Class<ValueType> getValueType() {
        return valueType;
    }

    public short getKeySize() {
        return keySize;
    }

    public short getValueSize() {
        return valueSize;
    }

    public short getMaxKeysN() {
        return maxKeysN;
    }

    public short getMinKeysN() {
        return minKeysN;
    }

    public CompareableArray<KeyType> getKeys() {
        return keys;
    }

    public Array<ValueType> getValues() {
        return values;
    }

    public KeyType getKey(int index) {
        return keys.get(index);
    }

    public ValueType getValue(int index) {
        return values.get(index);
    }

    public void setKeys(CompareableArray<KeyType> keys) {
        this.keys = keys;
    }

    public void setValues(Array<ValueType> values) {
        this.values = values;
    }

    public void setKey(int index, KeyType key) {
        keys.set(index, key);
    }

    public void setValue(int index, ValueType value) {
        values.set(index, value);
    }

    public void setKeysN(short keysN) {
        this.keysN = keysN;
        keys.setLength(keysN);
        values.setLength(keysN);
        writeHeader();
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
        writeHeader();
    }

    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
        writeHeader();
    }

    public void setNextLeafNode(long nextLeafNode) {
        this.nextLeafNode = nextLeafNode;
        writeHeader();
    }

}
