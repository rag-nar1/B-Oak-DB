package types;

import java.nio.ByteBuffer;

// static array
public class Array<T> {
    public interface MemoryCodec<T> {
        /** Number of bytes per element */
        int byteSize();

        /** Read the element at array‐index i from the buffer */
        T read(ByteBuffer buf, int i);

        /** Write the element at array‐index i into the buffer */
        void write(ByteBuffer buf, int i, T value);
    }

    private final ByteBuffer buf;
    private final int offsetBytes;
    private final int length;
    private final MemoryCodec<T> codec;

    public Array(byte[] data, MemoryCodec<T> codec, int offsetBytes, int length) {
        this.buf = ByteBuffer.wrap(data);
        this.offsetBytes = offsetBytes;
        this.length = length;
        this.codec = codec;
    }

    public int length() {
        return length;
    }

    /** Read element i (0 ≤ i < length) */
    public T get(int i) {
        checkIndex(i);
        return codec.read(buf.position(offsetBytes), i);
    }

    /** Write element i (0 ≤ i < length) */
    public void set(int i, T value) {
        checkIndex(i);
        codec.write(buf.position(offsetBytes), i, value);
    }

    private void checkIndex(int i) {
        if (i < 0 || i >= length)
        throw new IndexOutOfBoundsException(i + "/" + length);
    }


    // coders for primitive types // Todo: add StringCodec
    @SuppressWarnings("unchecked")
    public static <T> MemoryCodec<T> getCodec(Class<T> type) {
        if (type == Integer.class) {
            return (MemoryCodec<T>) new IntCodec();
        } else if (type == Long.class) {
            return (MemoryCodec<T>) new LongCodec();
        } else if (type == Double.class) {
            return (MemoryCodec<T>) new DoubleCodec();
        } else if (type == Short.class) {
            return (MemoryCodec<T>) new ShortCodec();
        } else if (type == Byte.class) {
            return (MemoryCodec<T>) new ByteCodec();
        } else if (type == Float.class) {
            return (MemoryCodec<T>) new FloatCodec();
        }
        throw new IllegalArgumentException("Unsupported type: " + type.getName());
    }

    public static class IntCodec implements MemoryCodec<Integer> {
        public int byteSize() {
            return Integer.BYTES;
        }

        public Integer read(ByteBuffer buf, int i) {
            return buf.getInt();
        }

        public void write(ByteBuffer buf, int i, Integer value) {
            buf.putInt(value);
        }
    }

    public static class LongCodec implements MemoryCodec<Long> {
        public int byteSize() {
            return Long.BYTES;
        }

        public Long read(ByteBuffer buf, int i) {
            return buf.getLong();
        }

        public void write(ByteBuffer buf, int i, Long value) {
            buf.putLong(value);
        }
    }

    public static class DoubleCodec implements MemoryCodec<Double> {
        public int byteSize() {
            return Double.BYTES;
        }

        public Double read(ByteBuffer buf, int i) {
            return buf.getDouble();
        }

        public void write(ByteBuffer buf, int i, Double value) {
            buf.putDouble(value);
        }
    }

    public static class ShortCodec implements MemoryCodec<Short> {
        public int byteSize() {
            return Short.BYTES;
        }

        public Short read(ByteBuffer buf, int i) {
            return buf.getShort();
        }

        public void write(ByteBuffer buf, int i, Short value) {
            buf.putShort(value);
        }
    }

    public static class ByteCodec implements MemoryCodec<Byte> {
        public int byteSize() {
            return Byte.BYTES;
        }

        public Byte read(ByteBuffer buf, int i) {
            return buf.get();
        }

        public void write(ByteBuffer buf, int i, Byte value) {
            buf.put(value);
        }
    }

    public static class FloatCodec implements MemoryCodec<Float> {
        public int byteSize() {
            return Float.BYTES;
        }

        public Float read(ByteBuffer buf, int i) {
            return buf.getFloat();
        }

        public void write(ByteBuffer buf, int i, Float value) {
            buf.putFloat(value);
        }
    }
}
