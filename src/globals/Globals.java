package globals;

import java.io.File;

public class Globals {
  public static final int PAGE_SIZE = 2 * 4096; // 4KB
  public static final int PRE_ALLOCATED_PAGES_COUNT = 1024; // 4Mb
  public static final long INVALID_PAGE_ID = -1;
  public static final int INVALID_Frame_ID = -1;

  public File logs;
}
