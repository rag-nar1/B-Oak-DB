package types;

import java.nio.ByteBuffer;

// types and interfaces
public class Key {
  byte[] val;
  Class<? extends Comparable<?>> type;
  MemoryCodec<? extends Comparable<?>> codec;

  public Key(byte[] val, Class<? extends Comparable<?>> type) {
    this.val = val;
    this.type = type;
    codec = getCodec(type);
  }

  public Key(Class<? extends Comparable<?>> type) {
    this.type = type;
    codec = getCodec(type);
  }

  public byte[] get() {
    return val;
  }

  public void set(byte[] val) {
    this.val = val;
  }

  public int compareTo(Key rhs) {
    if (type != rhs.type) {
      return Integer.MIN_VALUE;
    }
    return getVal().compareTo(rhs.getVal());
  }

  @SuppressWarnings("unchecked")
  public <T extends Comparable<T>> T getVal() {
    ByteBuffer buffer = ByteBuffer.wrap(val);
    if (type == Integer.class) {
      return (T) Integer.valueOf(buffer.getInt());
    } else if (type == Long.class) {
      return (T) Long.valueOf(buffer.getLong());
    } else if (type == Double.class) {
      return (T) Double.valueOf(buffer.getDouble());
    } else if (type == Short.class) {
      return (T) Short.valueOf(buffer.getShort());
    } else if (type == Byte.class) {
      return (T) Byte.valueOf(buffer.get());
    } else if (type == Float.class) {
      return (T) Float.valueOf(buffer.getFloat());
    }
    throw new IllegalArgumentException("Unsupported type: " + type.getName());
  }

  public int byteSize() {
    return codec.byteSize();
  }

  public void readVal(ByteBuffer buf) {
    val = codec.readRaw(buf);
  }

  public void write(ByteBuffer buf) {
    codec.write(buf, val);
  }

  public interface MemoryCodec<T> {
    /** Number of bytes per element */
    int byteSize();

    /** Read the element at array‐index i from the buffer */
    T read(ByteBuffer buf);

    /** Read the element at array‐index i from the buffer */
    byte[] readRaw(ByteBuffer buf);

    /** Write the element at array‐index i into the buffer */
    void write(ByteBuffer buf, byte[] value);
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

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
      buf.put(value);
    }
  }

  public static class LongCodec implements MemoryCodec<Long> {
    public int byteSize() {
      return Long.BYTES;
    }

    public Long read(ByteBuffer buf) {
      return buf.getLong();
    }

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
      buf.put(value);
    }
  }

  public static class DoubleCodec implements MemoryCodec<Double> {
    public int byteSize() {
      return Double.BYTES;
    }

    public Double read(ByteBuffer buf) {
      return buf.getDouble();
    }

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
      buf.put(value);
    }
  }

  public static class ShortCodec implements MemoryCodec<Short> {
    public int byteSize() {
      return Short.BYTES;
    }

    public Short read(ByteBuffer buf) {
      return buf.getShort();
    }

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
      buf.put(value);
    }
  }

  public static class ByteCodec implements MemoryCodec<Byte> {
    public int byteSize() {
      return Byte.BYTES;
    }

    public Byte read(ByteBuffer buf) {
      return buf.get();
    }

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
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

    public byte[] readRaw(ByteBuffer buf) {
      byte[] data = new byte[byteSize()];
      buf.get(data);
      return data;
    }

    public void write(ByteBuffer buf, byte[] value) {
      buf.put(value);
    }
  }
}
