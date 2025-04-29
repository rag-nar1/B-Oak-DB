package types;

import java.nio.ByteBuffer;
import java.util.List;

import types.Array.ByteCodec;
import types.Array.DoubleCodec;
import types.Array.FloatCodec;
import types.Array.IntCodec;
import types.Array.LongCodec;
import types.Array.ShortCodec;

public class Keys implements Comparable<Keys> {
    public interface MemoryCodec<T> {
        /** Number of bytes per element */
        int byteSize();

        /** Read the element at array‐index i from the buffer */
        T read(ByteBuffer buf);

        /** Write the element at array‐index i into the buffer */
        void write(ByteBuffer buf, T value);
    }

    public class Key<T extends Comparable<T>> implements Comparable<Key<T>> {
        T val;
        MemoryCodec<T> codec;

        @SuppressWarnings("unchecked")
        public Key(T val, Class<T> type) {
            this.val = val;
            
            if (type == Integer.class) {
                codec = (MemoryCodec<T>) new IntCodec();
            } else if (type == Long.class) {
                codec = (MemoryCodec<T>) new LongCodec();
            } else if (type == Double.class) {
                codec = (MemoryCodec<T>) new DoubleCodec();
            } else if (type == Short.class) {
                codec = (MemoryCodec<T>) new ShortCodec();
            } else if (type == Byte.class) {
                codec = (MemoryCodec<T>) new ByteCodec();
            } else if (type == Float.class) {
                codec = (MemoryCodec<T>) new FloatCodec();
            }
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
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
    }

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

    public Key<? extends Comparable<?>> get(int index) {
        return keys.get(index);
    }
}
