package diskmanager;

import java.io.Closeable;
import java.io.IOException;

public interface DiskManager extends Closeable {
  public void pushRequest(DiskRequest request) throws InterruptedException, NullPointerException;

  public long allocatePage(String fileName) throws IOException, NullPointerException;

  public int getFileCount();

  public long getPageCount(String fileName);

  public void open(String fileName) throws IOException, NullPointerException;
}
