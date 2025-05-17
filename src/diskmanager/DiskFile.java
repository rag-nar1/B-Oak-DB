package diskmanager;

import java.io.Closeable;
import java.io.IOException;

public interface DiskFile extends Closeable {

  /**
   * allocate new page in the file
   *
   * @return the page id of the allocated page
   * @throws IOException
   */
  public long allocatePage() throws IOException;

  /**
   * reads the content of the page into the destination
   *
   * @param pageID
   * @param data
   * @throws IOException
   */
  public void readPage(long pageID, byte[] dst) throws IOException;

  /**
   * writes the content of the src buffer into disk into the page (page id)
   *
   * @param pageID
   * @param src
   * @throws IOException
   */
  public void writePage(long pageID, byte[] src) throws IOException;

  /**
   * @return the page count of the file
   */
  public long getPageCnt();

  /**
   * @return the total file size in bytes
   */
  public long getFileSize();
}
