package types;

import java.nio.ByteBuffer;
import javax.naming.directory.InvalidAttributesException;

// static array
public class Array {

  private int length;
  private final int capacity;
  private final int offsetBytes;
  private final ByteBuffer buf;
  private final Template template;
  private final Compositekey keyUtil;

  public Array(Template template, byte[] data, int offsetBytes, int length, int capacity) {
    this.buf = ByteBuffer.wrap(data);
    this.offsetBytes = offsetBytes;
    this.length = length;
    this.capacity = capacity;
    this.template = template;
    this.keyUtil = new Compositekey(template);
  }

  public Array(Template template, ByteBuffer data, int offsetBytes, int length, int capacity) {
    this.buf = data;
    this.offsetBytes = offsetBytes;
    this.length = length;
    this.capacity = capacity;
    this.template = template;
    this.keyUtil = new Compositekey(template);
  }

  public int length() {
    return length;
  }

  /** Read element i (0 ≤ i < length) */
  public Compositekey get(int i) throws InvalidAttributesException {
    checkIndex(i);
    buf.position(offsetBytes + i * template.getByteSize());
    return keyUtil.read(buf);
  }

  /** Write element i (0 ≤ i < length) */
  public void set(int i, Compositekey value) {
    checkIndex(i);
    buf.position(offsetBytes + i * template.getByteSize());
    value.write(buf);
  }

  public void insert(int i, Compositekey value) throws InvalidAttributesException {
    if (length + 1 > capacity) throw new ArrayIndexOutOfBoundsException("Array is full");
    checkIndex(i);
    length++;
    // shift elements [i, length) to the right
    for (int j = length - 1; j > i; j--) {
      set(j, get(j - 1));
    }
    // insert new element
    set(i, value);
  }

  public void pushBack(Compositekey value) throws InvalidAttributesException {
    if (length + 1 > capacity) throw new ArrayIndexOutOfBoundsException("Array is full");

    set(length, value);
    length++;
  }

  public void delete(int index) throws InvalidAttributesException {
    checkIndex(index);
    for (int i = index; i < length - 1; i++) {
      set(i, get(i + 1));
    }
    length--;
  }

  private void checkIndexInbound(int i) {
    if (i < 0 || i >= length) throw new IndexOutOfBoundsException(i + "/" + length);
  }

  private void checkIndex(int i) {
    if (i < 0 || i >= capacity)
      throw new IndexOutOfBoundsException(i + "/c" + capacity + "/l" + length);
  }

  // getters and setters
  public ByteBuffer getBuf() {
    return buf;
  }

  public int getOffsetBytes() {
    return offsetBytes;
  }

  public int getLength() {
    return length;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setLength(int length) {
    this.length = length;
  }
}
