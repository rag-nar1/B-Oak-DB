package btree;

import types.Compositekey;

public interface Index {
  public Compositekey get(Compositekey key) throws Exception;

  public boolean insert(Compositekey key, Compositekey value) throws Exception;

  public boolean delete(Compositekey key) throws Exception;
}
