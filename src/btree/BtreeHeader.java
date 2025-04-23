package btree;

import java.nio.ByteBuffer;

import globals.Globals;

public class BtreeHeader {
    private long pageId;
    private long rootPageId;
    private short height;
    ByteBuffer buffer;
    public BtreeHeader() {
        this.pageId = -1;
        this.rootPageId = -1;
        this.height = 0;
    }

    public BtreeHeader(long pageId, long rootPageId, short height) {
        this.pageId = pageId;
        this.rootPageId = rootPageId;
        this.height = height;
    }

    public BtreeHeader(byte[] rowData) {
        buffer = ByteBuffer.wrap(rowData);
        this.pageId = buffer.getLong();
        this.rootPageId = buffer.getLong();
        this.height = buffer.getShort();
    }

    public BtreeHeader(ByteBuffer data) {
        buffer = data;
        this.pageId = buffer.getLong();
        this.rootPageId = buffer.getLong();
        this.height = buffer.getShort();
    }

    public void writeHeader() {
        buffer.rewind();
        buffer.putLong(pageId);
        buffer.putLong(rootPageId);
        buffer.putShort(height);
    }

    public long getPageId() {
        return pageId;
    }

    public void setPageId(long pageId) {
        this.pageId = pageId;
    }

    public long getRootPageId() {
        return rootPageId;
    }

    public void setRootPageId(long rootPageId) {
        this.rootPageId = rootPageId;
    }

    public short getHeight() {
        return height;
    }

    public void setHeight(short height) {
        this.height = height;
    }

    public boolean isEmpty() {
        return pageId == Globals.INVALID_PAGE_ID;
    }
}
