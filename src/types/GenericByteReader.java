package types;

import java.nio.ByteBuffer;

// Functional interface for converting bytes to a specific type T
@FunctionalInterface
interface ByteConverter<T> {
    T convert(byte[] bytes) throws Exception;
}

public class GenericByteReader {

    // Generic method to read bytes from disk and convert to type T
    public static <T> T convert(byte[] data, ByteConverter<T> converter) throws Exception {
        return converter.convert(data); // Convert bytes to type T
    }

    // generic method to convert a specific type T to bytes
    public static <T> byte[] convertToBytes(T value) {
        if (value instanceof Integer) {
            return ByteBuffer.allocate(4).putInt((Integer) value).array();
        } else if (value instanceof Long) {
            return ByteBuffer.allocate(8).putLong((Long) value).array();
        } else if (value instanceof Double) {
            return ByteBuffer.allocate(8).putDouble((Double) value).array();
        } else if (value instanceof Short) {
            return ByteBuffer.allocate(2).putShort((Short) value).array();
        } else if (value instanceof Byte) {
            return new byte[]{(Byte) value};
        } else if (value instanceof Float) {
            return ByteBuffer.allocate(4).putFloat((Float) value).array();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
        }
    }

    // Helper method to create converters for primitive wrapper types
    public static <T> ByteConverter<T> primitiveConverter(Class<T> targetType) {
        return bytes -> {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            if (targetType == Integer.class && bytes.length >= 4) {
                return targetType.cast(buffer.getInt());
            } else if (targetType == Long.class && bytes.length >= 8) {
                return targetType.cast(buffer.getLong());
            } else if (targetType == Double.class && bytes.length >= 8) {
                return targetType.cast(buffer.getDouble());
            } else if (targetType == Short.class && bytes.length >= 2) {
                return targetType.cast(buffer.getShort());
            } else if (targetType == Byte.class && bytes.length >= 1) {
                return targetType.cast(buffer.get());
            } else if (targetType == Float.class && bytes.length >= 4) {
                return targetType.cast(buffer.getFloat());
            } else {
                throw new IllegalArgumentException(
                    "Unsupported type " + targetType.getName() + " or insufficient bytes (length: " + bytes.length + ")"
                );
            }
        };
    }
}