package page;

import java.nio.ByteBuffer;

import javax.naming.directory.InvalidAttributesException;

import bufferpool.BufferPool;
import bufferpool.WriteGuard;
import globals.Globals;
import types.Array;
import types.CompareableArray;
import types.Compositekey;
import types.Template;

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
public class InternalNode extends TreeNodeHeader {
    private final short headerSize = 2 + 1 + 8; // 2 bytes for keysN, 1 byte for type, 8 bytes for pageId
    private final Template keyType;
    private final Template valueType;
    private final short keySize;
    private final short valueSize;
    private final short maxKeysN;
    private final short minKeysN;
    private CompareableArray keys;
    private Array values; // pageIds of the child nodes
    private ByteBuffer buffer;

    public InternalNode(Template keyType, long pageId) {
        super(pageId, false);
        this.keyType = keyType;
        this.valueType = new Template(Long.class);
        keySize = keyType.getByteSize();
        valueSize = valueType.getByteSize();
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);
    }

    public InternalNode(Template keyType, ByteBuffer rawData) {
        this.keyType = keyType;
        this.valueType = new Template(Long.class);
        keySize = keyType.getByteSize();
        valueSize = valueType.getByteSize();
        maxKeysN = (short) ((Globals.PAGE_SIZE - headerSize) / (keySize + valueSize));
        minKeysN = (short) (maxKeysN / 2);

        buffer = rawData;
        this.keysN = buffer.getShort();
        this.isLeaf = buffer.get() == 1;
        this.pageId = buffer.getLong();

        keys = new CompareableArray(new Compositekey(keyType), rawData, headerSize, keysN, maxKeysN);
        values = new Array(new Compositekey(valueType),rawData, headerSize + maxKeysN * keySize, keysN, maxKeysN);
    }

    public InternalNode(Template keyType, byte[] rawData) {
        this(keyType, ByteBuffer.wrap(rawData));
    }

    public void writeHeader() {
        buffer.rewind();
        buffer.putShort(keysN);
        buffer.put((byte) (isLeaf ? 1 : 0));
        buffer.putLong(pageId);
    }

    public Compositekey getChildForKey(Compositekey key) throws InvalidAttributesException{
        int index = keys.lowerBound(key, 1);
        return values.get(index - 1);
    }

    public int getKeyIdx(Compositekey key) throws InvalidAttributesException{
        int index = keys.lowerBound(key, 1);
        return index;
    }

    public boolean insert(Compositekey key, Compositekey value) throws InvalidAttributesException {
        if (keysN == maxKeysN) {
            return false; // node is full
        }
        int index = keys.upperBound(key, 1);
        keys.insert(index, key);
        values.insert(index - 1, value);
        keysN++;
        writeHeader();
        return true;
    }

    public void delete(int index) throws InvalidAttributesException {
        keys.delete(index);
        values.delete(index);
        if (keysN > 0) {
            keysN--;
            writeHeader();
        }
    }

    public void deleteRespective(int index) throws InvalidAttributesException {
        keys.delete(index);
        values.delete(index - 1);
        if (keysN > 0) {
            keysN--;
            writeHeader();
        }
    }

    public WriteGuard split(BufferPool bufferPool, String fileName) {
        if (keysN < minKeysN) {
            return null;
        }

        try {
            long newPageId = bufferPool.allocateNewPage(fileName);
            WriteGuard newGuard = bufferPool.getWriteGuard(fileName, newPageId);
            InternalNode newNode = new InternalNode(keyType, newGuard.getDataMut());
            newNode.setLeaf(false);
            newNode.setPageId(newPageId);

            for (int i = minKeysN; i < keysN; i++) {
                newNode.setKey(i - minKeysN, keys.get(i));
                newNode.setValue(i - minKeysN, values.get(i));
            }
            newNode.setCompositekeyN((short) (keysN - minKeysN));
            setCompositekeyN(minKeysN);

            newNode.writeHeader();
            writeHeader();
            return newGuard;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    public Template getKeyType() {
        return keyType;
    }

    public CompareableArray getCompositekey() {
        return keys;
    }

    public Array getValues() {
        return values;
    }

    public void setCompositekey(CompareableArray keys) {
        this.keys = keys;
    }

    public void setValues(Array values) {
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

    public void setCompositekeyN(short keysN) {
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

    public Compositekey getKey(int index) throws InvalidAttributesException{
        return keys.get(index);
    }

    public Compositekey getValue(int index) throws InvalidAttributesException{
        return values.get(index);
    }

    public void setKey(int index, Compositekey key) {
        keys.set(index, key);
    }

    public void setValue(int index, Compositekey value) {
        values.set(index, value);
    }
}
