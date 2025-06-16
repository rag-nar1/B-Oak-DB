package types;

public class Types {

  public static final short INT_SIZE = 4;
  public static final short LONG_SIZE = 8;
  public static final short DOUBLE_SIZE = 8;
  public static final short SHORT_SIZE = 2;
  public static final short BYTE_SIZE = 1;
  public static final short FLOAT_SIZE = 4;
  public static final short JSON_SIZE = 2 * 1024; // 2KB
  // Add more types as needed

  public static short getSize(Class<?> type) {
    if (type == Integer.class) {
      return INT_SIZE;
    } else if (type == Long.class) {
      return LONG_SIZE;
    } else if (type == Double.class) {
      return DOUBLE_SIZE;
    } else if (type == Short.class) {
      return SHORT_SIZE;
    } else if (type == Byte.class) {
      return BYTE_SIZE;
    } else if (type == Float.class) {
      return FLOAT_SIZE;
    } else if (type == Json.class) {
      return JSON_SIZE;
    }
    // Add more types as needed
    throw new IllegalArgumentException("Unsupported type: " + type.getName());
  }
}
