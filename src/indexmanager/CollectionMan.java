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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import globals.*;

public class CollectionMan implements Closeable {
  private final static String fileSuffix = ".man";
  private static String fileName;
  private static ObjectInputStream reader;
  private static ObjectOutputStream writer;
  private static List<String> indexes;

  public CollectionMan(String collectionName) throws IOException,  ClassNotFoundException{
    fileName = collectionName + fileSuffix;
    initRW();
  }

  public void initRW() throws IOException,  ClassNotFoundException{
    try {
      reader = new ObjectInputStream(new FileInputStream(Globals.STORAGE_DIR + fileName));
      writer = new ObjectOutputStream(new FileOutputStream(Globals.STORAGE_DIR + fileName));
      read();
    } catch (FileNotFoundException e) {
      indexes = new LinkedList<String>();
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
    indexes = (List<String>) reader.readObject();
  }
}
