package test.types;


import static org.junit.Assert.assertEquals;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Test;

import types.Array;
import types.Compositekey;
import types.Template;

public class ArrayTest {

    @Test
    public void testBasic() throws InvalidAttributesException {
        byte[] buffer = new byte[4096];
        Template type = new Template(Integer.class);
        int n = 4096 / 4;
        Array arr = new Array(type, buffer, 0, 0, n);
        for (int i = 0; i < n; i ++) {
            Compositekey key = new Compositekey(type);
            key.set(0, i, Integer.class);
            arr.insert(i, key);
        }

        for (int i = 0; i < n; i ++) {
            Compositekey key = arr.get(i);
            int val = key.getVal(0);
            assertEquals(i, val);
        }
    }
    
}
