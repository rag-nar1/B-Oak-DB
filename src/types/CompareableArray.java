package types;

import java.nio.ByteBuffer;
import javax.naming.directory.InvalidAttributesException;

public class CompareableArray extends Array {
  public CompareableArray(
      Template template, byte[] data, int offsetBytes, int length, int capacity) {
    super(template, data, offsetBytes, length, capacity);
  }

  public CompareableArray(
      Template template, ByteBuffer data, int offsetBytes, int length, int capacity) {
    super(template, data, offsetBytes, length, capacity);
  }

  public int upperBound(Compositekey key, int low, int high) throws InvalidAttributesException {
    if (high < low) {
      return low;
    }
    int index = high;
    while (low <= high) {
      int mid = (low + high) / 2;
      Compositekey midKey = get(mid);
      if (midKey.compareTo(key) > 0) {
        index = mid;
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }
    Compositekey curr = get(index);
    if (curr.compareTo(key) <= 0) {
      index++;
    }
    return index;
  }

  public int lowerBound(Compositekey key, int low, int high) throws InvalidAttributesException {
    if (high < low) {
      return low;
    }
    int index = high;
    while (low <= high) {
      int mid = (low + high) / 2;
      Compositekey midKey = get(mid);
      if (midKey.compareTo(key) >= 0) {
        index = mid;
        high = mid - 1;
      } else {
        low = mid + 1;
      }
    }
    Compositekey curr = get(index);
    if (curr.compareTo(key) < 0) {
      index++;
    }
    return index;
  }

  public int upperBound(Compositekey key) throws InvalidAttributesException {
    return upperBound(key, 0, length() - 1);
  }

  public int upperBound(Compositekey key, int low) throws InvalidAttributesException {
    return upperBound(key, low, length() - 1);
  }

  public int lowerBound(Compositekey key) throws InvalidAttributesException {
    return lowerBound(key, 0, length() - 1);
  }

  public int lowerBound(Compositekey key, int low) throws InvalidAttributesException {
    return lowerBound(key, low, length() - 1);
  }

  public int binarySearch(Compositekey key, int low, int high) throws InvalidAttributesException {
    while (low <= high) {
      int mid = (low + high) / 2;
      Compositekey midKey = get(mid);
      if (midKey.compareTo(key) == 0) {
        return mid; // key found
      } else if (midKey.compareTo(key) < 0) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return 0; // key not found
  }

  public int binarySearch(Compositekey key) throws InvalidAttributesException {
    return binarySearch(key, 0, length() - 1);
  }

  public int binarySearch(Compositekey key, int low) throws InvalidAttributesException {
    return binarySearch(key, low, length() - 1);
  }
}
