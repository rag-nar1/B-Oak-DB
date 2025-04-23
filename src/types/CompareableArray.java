package types;

public class CompareableArray <T extends Comparable<T>> extends Array<T> {
    public CompareableArray(byte[] data, MemoryCodec<T> codec, int offsetBytes, int length) {
        super(data, codec, offsetBytes, length);
    }

    public int upperBound(T key) {
        int index = 0;
        int low = 0, high = length() - 1;
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
}