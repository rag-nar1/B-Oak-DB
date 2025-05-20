package globals;

public class Globals {
  public static final int PAGE_SIZE = 2 * 4096; // 8KB
  public static final int CLUSTER_PAGE_SIZE = 4 * 4096; // 16KB
  public static final int PRE_ALLOCATED_PAGES_COUNT = 1024; // 8Mb
  public static final long INVALID_PAGE_ID = -1;
  public static final int INVALID_Frame_ID = -1;
  public static final String STORAGE_DIR = "storage/";
}
