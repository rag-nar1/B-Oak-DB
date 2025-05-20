package indexmanager;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import globals.*;

public class CollectionMan implements Closeable {
  private final static String fileSuffix = ".man";
  private static String fileName;
  private static String collection;
  private static ObjectInputStream reader;
  private static ObjectOutputStream writer;
  private static Set<String> indexes;

  public CollectionMan(String collectionName) throws IOException,  ClassNotFoundException{
    collection = collectionName;
    fileName = collectionName + fileSuffix;
    initRW();
  }

  public void initRW() throws IOException,  ClassNotFoundException{
    try {
      reader = new ObjectInputStream(new FileInputStream(Globals.STORAGE_DIR + fileName));
      writer = new ObjectOutputStream(new FileOutputStream(Globals.STORAGE_DIR + fileName));
      read();
    } catch (FileNotFoundException e) {
      indexes = new HashSet<String>();
      Path filePath = Paths.get(Globals.STORAGE_DIR + fileName);
      Files.createFile(filePath);
      reader = new ObjectInputStream(new FileInputStream(Globals.STORAGE_DIR + fileName));
      writer = new ObjectOutputStream(new FileOutputStream(Globals.STORAGE_DIR + fileName));
    }
  }

  public void close() throws IOException {
    flush();
  }

  private static void flush() throws IOException {
    writer.writeObject(indexes);
  }

  @SuppressWarnings("unchecked")
  private static void read() throws IOException, ClassNotFoundException {
    indexes = (Set<String>) reader.readObject();
  }

  public boolean hasIndex(String indexName) {
    return indexes.contains(indexName);
  }

  public String getIndexForField(String field) {
    String exepectedIndexName = collection + "-" + field;
    if (indexes.contains(exepectedIndexName)) {
      return exepectedIndexName;
    }
    return null;
  }
}
