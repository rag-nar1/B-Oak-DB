package bufferpool;

public interface Replacer {
    public void recordAccess(int frameId);
    public int evict();
    public void setEvictable(int frameId, boolean evictable);
    public void deleteFrame(int frameId);
}
