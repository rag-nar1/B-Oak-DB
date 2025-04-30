package test.types;


import javax.naming.directory.InvalidAttributesException;

import org.junit.Test;

import types.Array;
import types.Template;

public class ArrayTest {

    @Test
    public void testBasic() throws InvalidAttributesException {
        byte[] buffer = new byte[4096];
        Template type = new Template(Integer.class);
        int n = 4096 / 4;
        Array arr = new Array(new Keys(type), buffer, 0, 0, n);
        for(int i = 0; i < n; i ++) {
            arr.insert(i, new Keys(type, new Key<Integer>(i, Integer.class)));
        }
    }
    
}
