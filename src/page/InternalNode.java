package page;

import java.nio.ByteBuffer;

import globals.Globals;
import types.Array;
import types.CompareableArray;
import types.Types;

/**
 * InternalNode class represents an internal node in a B+ tree.
 * It extends the TreeNode class and is used to store keys and child pointers.
 * The keys are used to navigate the tree, while the child pointers are pageIds
 * that point to the child nodes.
 * | null |key2 |key3 |...|keyN |
 * |pageId1|pageId2|pageId3|...|pageIdN|
 * pageId_i points to the subtree where keys there sutisfy key_i <= key < key_(i+1)
 * for i = 0, key_0 = -inf
 * for i = N, key_N = +inf
 */
public class InternalNode<KeyType extends Comparable<KeyType>> extends TreeNodeHeader {
    private final short headerSize = 2 + 1 + 8; // 2 bytes for keysN, 1 byte for type, 8 bytes for pageId
    private final Class<KeyType> keyType;
    private final short keySize;
    private final short maxKeysN;
    private final short minKeysN;
    private CompareableArray<KeyType> keys;
    private Array<Long> values; // pageIds of the child nodes
    private ByteBuffer buffer;

    public InternalNode(Class<KeyType> keyType, long pageId) {
        super(pageId, false);
        this.keyType = keyType;
        keySize = (short) Types.getSize(keyType);
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + 8));
        minKeysN = (short) (maxKeysN / 2);
        keys = new CompareableArray<KeyType>(null, Array.getCodec(keyType), headerSize, keysN, maxKeysN);
        values = new Array<>(null, Array.getCodec(Long.class), headerSize,  keysN, maxKeysN);
    }

    public InternalNode(Class<KeyType> keyType, byte[] rawData) {
        this.keyType = keyType;
        keySize = Types.getSize(keyType);
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + Long.BYTES));
        minKeysN = (short) (maxKeysN / 2);

        buffer = ByteBuffer.wrap(rawData);
        this.keysN = buffer.getShort();
        this.isLeaf = buffer.get() == 1;
        this.pageId = buffer.getLong();

        keys = new CompareableArray<KeyType>(rawData, Array.getCodec(keyType), headerSize, keysN, maxKeysN);
        values = new Array<>(rawData, Array.getCodec(Long.class), headerSize + keysN * keySize, keysN, maxKeysN);
    }

    public void writeHeader() {
        buffer.rewind();
        buffer.putShort(keysN);
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putLong(pageId);
    }

    public long getChildForKey(KeyType key) {
        int index = keys.upperBound(key, 1);
        return values.get(index - 1);
    }


    // Getters and Setters
    public short getHeaderSize() {
        return headerSize;
    }

    public short getMaxKeysN() {
        return maxKeysN;
    }

    public short getMinKeysN() {
        return minKeysN;
    }

    public short getKeySize() {
        return keySize;
    }

    public Class<KeyType> getKeyType() {
        return keyType;
    }

    public CompareableArray<KeyType> getKeys() {
        return keys;
    }

    public Array<Long> getValues() {
        return values;
    }

    public void setKeys(CompareableArray<KeyType> keys) {
        this.keys = keys;
    }

    public void setValues(Array<Long> values) {
        this.values = values;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] rawData) {
        this.buffer = ByteBuffer.wrap(rawData);
    }

    public void setKeysN(short keysN) {
        this.keysN = keysN;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public void setLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public KeyType getKey(int index) {
        return keys.get(index);
    }

    public Long getValue(int index) {
        return values.get(index);
    }

    public void setKey(int index, KeyType key) {
        keys.set(index, key);
    }

    public void setValue(int index, Long value) {
        values.set(index, value);
    }
}
