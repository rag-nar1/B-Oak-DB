package page;

import java.nio.ByteBuffer;

/**
 * TreeNodeHeader class represents a node in a B+ tree. It is a generic class that can be used for
 * both internal and leaf nodes. The keys are used to navigate the tree, while the values are used
 * to store data. on disk layout: | 2bytes | 1byte | 8 bytes | keys... | values... | | KeysN | Type
 * | pageId | key1 | key2 | ... | keyN | value1 | value2 | ... | valueN |
 */
public class TreeNodeHeader {
  protected short keysN;
  protected long pageId;
  protected boolean isLeaf;
  protected ByteBuffer buffer;

  public TreeNodeHeader() {}

  public TreeNodeHeader(long pageId, boolean isLeaf) {
    keysN = 0;
    this.isLeaf = isLeaf;
    this.pageId = pageId;
  }

  public TreeNodeHeader(byte[] rowData) {
    buffer = ByteBuffer.wrap(rowData);
    this.keysN = buffer.getShort();
    this.isLeaf = buffer.get() == 1;
    this.pageId = buffer.getLong();
  }

  // getters and setters

  public short getKeysN() {
    return keysN;
  }

  public long getPageId() {
    return pageId;
  }

  public boolean isLeaf() {
    return isLeaf;
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
}
