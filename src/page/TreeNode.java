package page;

import java.io.Serializable;

/**
 * TreeNode class represents a node in a B+ tree.
 * It is a generic class that can be used for both internal and leaf nodes.
 * The keys are used to navigate the tree, while the values are used to store data.
 on disk layout:
    * | 2bytes | 2bytes | 2bytes | 1byte | 8 bytes | keys... | values... |
    *| KeysN  | keySize|valueSize| Type  | pageId | key1    | key2    | ... | keyN    | value1 | value2 | ... | valueN | 
 */
public class TreeNode<KeyType extends Comparable<KeyType> & Serializable, ValueType extends Serializable> implements Serializable {
    protected short keysN;
    protected short keySize; // size of the key in bytes
    protected short valueSize; // size of the value in bytes
    protected long pageId;
    protected boolean isLeaf;
    protected KeyType[] keys;
    protected ValueType[] values;
    protected short maxKeysN; // maximum number of keys = (4096 - header size) / (key size + value size)
    protected short minKeysN; // minimum number of keys = maxKeysN / 2

    public TreeNode() {}

    @SuppressWarnings("unchecked")
    public TreeNode(long pageId, short keySize, short valueSize, boolean isLeaf) {
        keysN = 0;
        this.isLeaf = isLeaf;
        this.pageId = pageId;
        this.keySize = keySize;
        this.valueSize = valueSize;
        // Assuming that KeyType and ValueType are reference types
        keys = (KeyType[]) new Comparable[keysN];
        values = (ValueType[]) new Object[keysN];
    }

    public short getKeysN() {
        return keysN;
    }

    public KeyType[] getKeys() {
        return keys;
    }

    public ValueType[] getValues() {
        return values;
    }

    public void setKey(int index, KeyType key) {
        if (index > 0 && index < keysN) {
            keys[index] = key;
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public void setValue(int index, ValueType value) {
        if (index >= 0 && index < keysN) {
            values[index] = value;
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public KeyType getKey(int index) {
        if (index > 0 && index < keysN) {
            return keys[index];
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public ValueType getValue(int index) {
        if (index >= 0 && index < keysN) {
            return values[index];
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
    }

    public void setKeysN(short keysN) {
        this.keysN = keysN;
    }

    public void setKeys(KeyType[] keys) {
        this.keys = keys;
    }

    public void setValues(ValueType[] values) {
        this.values = values;
    }

}
