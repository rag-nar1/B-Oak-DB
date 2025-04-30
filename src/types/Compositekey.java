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
            this.keys[i] = new Key(classes.getClass(i));
        }
    }

    public Compositekey(Class<? extends Comparable<?>>... classes) {
        super(classes);
        this.keys = new Key[classes.length];
    }

    public Compositekey(Template classes) {
        super(classes.classes);
        this.keys = new Key[this.classes.length];
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
            buf.position(buf.position() + keys[i].byteSize()); // move the position in the buffer forward
        }
        return this;
    }

    public void write(ByteBuffer buf) {
        for (int i = 0; i < keys.length; i++) {
            keys[i].write(buf);
            buf.position(buf.position() + keys[i].byteSize()); // move the position in the buffer forward   
        }
    }

    public Key get(int index) {
        return keys[index];
    }

    @SuppressWarnings("unchecked")
    public <T> T getVal(int index) {
        return (T) keys[index].getVal();
    }
}
