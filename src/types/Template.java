package types;

public class Template {
  public Class<? extends Comparable<?>>[] classes;
  public short byteSize;

  public Template(Class<? extends Comparable<?>>... classes) {
    this.classes = classes;
    this.byteSize = byteSize();
  }

  public short getByteSize() {
    return byteSize;
  }

  public Class<? extends Comparable<?>> getClass(int index) {
    return classes[index];
  }

  private short byteSize() {
    short size = 0;
    for (int i = 0; i < classes.length; i++) {
      size += typeByteSize(classes[i]);
    }
    return size;
  }

  public short typeByteSize(int index) {
    Class<?> type = classes[index];
    if (type == Integer.class) {
      return Integer.BYTES;
    } else if (type == Long.class) {
      return Long.BYTES;
    } else if (type == Double.class) {
      return Double.BYTES;
    } else if (type == Short.class) {
      return Short.BYTES;
    } else if (type == Byte.class) {
      return Byte.BYTES;
    } else if (type == Float.class) {
      return Float.BYTES;
    } else if (type == Json.class) {
      return Types.JSON_SIZE;
    }
    throw new IllegalArgumentException("Unsupported type: " + type.getName());
  }

  public short typeByteSize(Class<?> type) {
    if (type == Integer.class) {
      return Integer.BYTES;
    } else if (type == Long.class) {
      return Long.BYTES;
    } else if (type == Double.class) {
      return Double.BYTES;
    } else if (type == Short.class) {
      return Short.BYTES;
    } else if (type == Byte.class) {
      return Byte.BYTES;
    } else if (type == Float.class) {
      return Float.BYTES;
    } else if (type == Json.class) {
      return Types.JSON_SIZE;
    }
    throw new IllegalArgumentException("Unsupported type: " + type.getName());
  }
}
