package types;

import java.nio.ByteBuffer;

import javax.naming.directory.InvalidAttributesException;


public class Compositekey extends Template implements Comparable<Compositekey>{

    // List of Types
    private Key[] keys; 

    @SuppressWarnings("unchecked")
    public Compositekey(Template classes, Key... keys) throws InvalidAttributesException{
        super(classes.classes);
        if (classes.classes.length != keys.length) {
            throw new InvalidAttributesException();
        }
        this.keys = new Key[keys.length];
        for (int i = 0; i < keys.length; i ++) {
            this.keys[i] = new Key(getClass(i));
        }
    }

    public Compositekey(Class<? extends Comparable<?>>... classes) {
        super(classes);
        this.keys = new Key[classes.length];
        for (int i = 0; i < keys.length; i ++) {
            this.keys[i] = new Key(getClass(i));
        }
    }

    public Compositekey(Template classes) {
        super(classes.classes);
        this.keys = new Key[this.classes.length];
        for (int i = 0; i < keys.length; i ++) {
            this.keys[i] = new Key(getClass(i));
        }
    }

    public int compareTo(Compositekey rhs) {
        for (int i = 0; i < keys.length; i ++) {
            Key key1 = get(i);
            Key key2 = rhs.get(i);
            int cmp = key1.compareTo(key2);
            if(cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public Compositekey read(ByteBuffer buf) throws InvalidAttributesException {
        for (int i = 0; i < keys.length; i++) {
            keys[i].readVal(buf);
        }
        return this;
    }

    public void write(ByteBuffer buf) {
        for (int i = 0; i < keys.length; i++) {
            keys[i].write(buf); 
        }
    }

    public Key get(int index) {
        return keys[index];
    }

    public void set(int index, byte[] val) {
        keys[index].set(val);
    }

    public <T> void set(int index, T val, Class<T> type) {
        ByteBuffer buf;
        if (type == Integer.class) {
            buf = ByteBuffer.wrap(new byte[Integer.BYTES]);
            buf.putInt((int) val);
        } else if (type == Long.class) {
            buf = ByteBuffer.wrap(new byte[Long.BYTES]);
            buf.putLong((long) val);
        } else if (type == Double.class) {
            buf = ByteBuffer.wrap(new byte[Double.BYTES]);
            buf.putDouble((double) val);
        } else if (type == Short.class) {
            buf = ByteBuffer.wrap(new byte[Short.BYTES]);
            buf.putShort((Short) val);
        } else if (type == Byte.class) {
            buf = ByteBuffer.wrap(new byte[Byte.BYTES]);
            buf.put((Byte) val);
        } else if (type == Float.class) {
            buf = ByteBuffer.wrap(new byte[Float.BYTES]);
            buf.putFloat((Float) val);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
        keys[index].set(buf.array());
    }

    @SuppressWarnings("unchecked")
    public <T> T getVal(int index) {
        return (T) keys[index].getVal();
    }
}
