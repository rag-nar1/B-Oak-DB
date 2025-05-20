package indexmanager;

import java.util.Map;
import java.io.*;
import bufferpool.BufferPool;

public class IndexMan implements Closeable {

  private final static String filename = "index.man";
  private final static String headerfilename = "indexheader.man";
  private BufferPool bufferPool;
  private static Map<String, Long> indexPageId;

  public IndexMan(BufferPool bufferPool) throws IOException, ClassNotFoundException {
    this.bufferPool = bufferPool;
    loadMap();
  }

  public void close() throws IOException {
    saveMap();
  }

  public long getCollectionManHeadPage(String collectionName) {
    return indexPageId.get(collectionName);
  }

  private static void saveMap() throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
      oos.writeObject(indexPageId);
    }
  }

  @SuppressWarnings("unchecked")
  private static void loadMap() throws IOException, ClassNotFoundException {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
      indexPageId =  (Map<String, Long>) ois.readObject();
    }
  }
}