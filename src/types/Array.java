package types;

import java.nio.ByteBuffer;

// static array
public class Array<T> {
    public interface MemoryCodec<T> {
        /** Number of bytes per element */
        int byteSize();

        /** Read the element at array‐index i from the buffer */
        T read(ByteBuffer buf);

        /** Write the element at array‐index i into the buffer */
        void write(ByteBuffer buf, T value);
    }

    private final ByteBuffer buf;
    private final int offsetBytes;
    private int length;
    private final int capacity;
    private final MemoryCodec<T> codec;

    public Array(byte[] data, MemoryCodec<T> codec, int offsetBytes, int length, int capacity) {
        this.buf = ByteBuffer.wrap(data);
        this.offsetBytes = offsetBytes;
        this.length = length;
        this.capacity = capacity;
        this.codec = codec;
    }

    public Array(ByteBuffer data, MemoryCodec<T> codec, int offsetBytes, int length, int capacity) {
        this.buf = data;
        this.offsetBytes = offsetBytes;
        this.length = length;
        this.capacity = capacity;
        this.codec = codec;
    }

    public int length() {
        return length;
    }

    /** Read element i (0 ≤ i < length) */
    public T get(int i) {
        checkIndex(i);
        buf.position(offsetBytes + i * codec.byteSize());
        return codec.read(buf);
    }

    /** Write element i (0 ≤ i < length) */
    public void set(int i, T value) {
        checkIndex(i);
        buf.position(offsetBytes + i * codec.byteSize());
        codec.write(buf, value);
    }

    public void insert(int i, T value) {
        if (length + 1 > capacity)
            throw new ArrayIndexOutOfBoundsException("Array is full");
        checkIndex(i);
        length++;
        // shift elements [i, length) to the right
        for (int j = length - 1; j > i; j--) {
            set(j, get(j - 1));
        }
        // insert new element
        set(i, value);
    }

    private void checkIndexInbound(int i) {
        if (i < 0 || i >= length)
            throw new IndexOutOfBoundsException(i + "/" + length);
    }

    private void checkIndex(int i) {
        if (i < 0 || i >= capacity)
            throw new IndexOutOfBoundsException(i + "/" + length);
    }

    // getters and setters
    public ByteBuffer getBuf() {
        return buf;
    }

    public int getOffsetBytes() {
        return offsetBytes;
    }
    
    public int getLength() {
        return length;
    }

    public int getCapacity() {
        return capacity;
    }

    public MemoryCodec<T> getCodec() {
        return codec;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
