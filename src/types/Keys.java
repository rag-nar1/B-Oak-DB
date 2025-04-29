package types;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class Keys implements Comparable<Keys> {

    // List of Types
    private List<Key<? extends Comparable<?>>> keys; 
    private List<Class<?>> classes;
    public Keys(int n, List<Key<? extends Comparable<?>>> keys) {
        this.keys = keys;
        for (int i = 0; i < keys.size(); i ++) {
            classes.set(i,  keys.get(i).getClass());
        }
    }

    @SuppressWarnings("unchecked")
    public int compareTo(Keys rhs) {
        for (int i = 0; i < keys.size(); i ++) {
            Comparable<Object> key1 = (Comparable<Object>) get(i).get();
            Comparable<Object> key2 = (Comparable<Object>) rhs.get(i).get();
            
            int cmp = key1.compareTo(key2);
            if(cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public int byteSize() {
        int size = 0;
        for (int i = 0; i < keys.size(); i ++) {
            size += keys.get(i).byteSize();
        }
        return size;
    }

    @SuppressWarnings("unchecked")
    public Keys read(ByteBuffer buf) {
        List<Key<? extends Comparable<?>>> readKeys = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            Comparable<?> value = keys.get(i).read(buf);
            Key<?> key = new Key((Comparable<?>) value, (Class<Comparable<?>>) classes.get(i));
            readKeys.add(key);
            buf.position(buf.position() + keys.get(i).byteSize()); // move the position in the buffer forward
        }
        return new Keys(readKeys.size(), readKeys);
    }

    public void write(ByteBuffer buf) {
        for (int i = 0; i < keys.size(); i++) {
            keys.get(i).write(buf);
            buf.position(buf.position() + keys.get(i).byteSize()); // move the position in the buffer forward   
        }
    }

    public Key<? extends Comparable<?>> get(int index) {
        return keys.get(index);
    }


    // types and interfaces
    public class Key<T extends Comparable<T>> implements Comparable<Key<T>> {
        T val;
        MemoryCodec<T> codec;

        public Key(T val, Class<T> type) {
            this.val = val;
            codec = getCodec(type);
        }

        public T get() {
            return val;
        }

        public void get(T val) {
            this.val = val;
        }

        public int compareTo(Key<T> rhs) {
            return val.compareTo(rhs.val);
        }

        public int byteSize() {
            return codec.byteSize();
        }

        public T read(ByteBuffer buf) {
            return codec.read(buf);
        }

        public void write(ByteBuffer buf) {
            codec.write(buf, val);
        }

    }

    public interface MemoryCodec<T> {
        /** Number of bytes per element */
        int byteSize();

        /** Read the element at array‐index i from the buffer */
        T read(ByteBuffer buf);

        /** Write the element at array‐index i into the buffer */
        void write(ByteBuffer buf, T value);
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

        public Integer read(ByteBuffer buf) {
            return buf.getInt();
        }

        public void write(ByteBuffer buf, Integer value) {
            buf.putInt(value);
        }
    }

    public static class LongCodec implements MemoryCodec<Long> {
        public int byteSize() {
            return Long.BYTES;
        }

        public Long read(ByteBuffer buf) {

            return buf.getLong();
        }

        public void write(ByteBuffer buf, Long value) {
            buf.putLong(value);
        }
    }

    public static class DoubleCodec implements MemoryCodec<Double> {
        public int byteSize() {
            return Double.BYTES;
        }

        public Double read(ByteBuffer buf) {
            return buf.getDouble();
        }

        public void write(ByteBuffer buf, Double value) {
            buf.putDouble(value);
        }
    }

    public static class ShortCodec implements MemoryCodec<Short> {
        public int byteSize() {
            return Short.BYTES;
        }

        public Short read(ByteBuffer buf) {
            return buf.getShort();
        }

        public void write(ByteBuffer buf, Short value) {
            buf.putShort(value);
        }
    }

    public static class ByteCodec implements MemoryCodec<Byte> {
        public int byteSize() {
            return Byte.BYTES;
        }

        public Byte read(ByteBuffer buf) {
            return buf.get();
        }

        public void write(ByteBuffer buf, Byte value) {
            buf.put(value);
        }
    }

    public static class FloatCodec implements MemoryCodec<Float> {
        public int byteSize() {
            return Float.BYTES;
        }

        public Float read(ByteBuffer buf) {
            return buf.getFloat();
        }

        public void write(ByteBuffer buf, Float value) {
            buf.putFloat(value);
        }
    }
}
