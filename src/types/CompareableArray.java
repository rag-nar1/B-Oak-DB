package types;

public class CompareableArray <T extends Comparable<T>> extends Array<T> {
    public CompareableArray(byte[] data, MemoryCodec<T> codec, int offsetBytes, int length, int capacity) {
        super(data, codec, offsetBytes, length, capacity);
    }

    public int upperBound(T key, int low, int high) {
        int index = low;
        while (low <= high) {
            int mid = (low + high) / 2;
            T midKey = get(mid);
            if (midKey.compareTo(key) > 0) {
                index = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return index;
    }
    
    public int upperBound(T key) {
        return upperBound(key, 0, length() - 1);
    }

    public int upperBound(T key, int low) {
        return upperBound(key, low, length() - 1);
    }

}