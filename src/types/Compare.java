package types;

// generic compare class to compare two objects of type T where T is primitive type
public class Compare <T> {
    public static <T> int compare(T a, T b) { 
        if (a instanceof Integer) {
            return Integer.compare((Integer) a, (Integer) b);
        } else if (a instanceof Long) {
            return Long.compare((Long) a, (Long) b);
        } else if (a instanceof Double) {
            return Double.compare((Double) a, (Double) b);
        } else if (a instanceof Short) {
            return Short.compare((Short) a, (Short) b);
        } else if (a instanceof Byte) {
            return Byte.compare((Byte) a, (Byte) b);
        } else if (a instanceof Float) {
            return Float.compare((Float) a, (Float) b);
        } else if (a instanceof String) {
            return ((String) a).compareTo((String) b);
        } else if (a instanceof Character) {
            return Character.compare((Character) a, (Character) b);
        } else if (a instanceof Boolean) {
            return Boolean.compare((Boolean) a, (Boolean) b);
        }
        throw new IllegalArgumentException("Unsupported type: " + a.getClass().getName());
    }
}
