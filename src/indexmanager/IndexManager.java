package indexmanager;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * index naming would be in this format
 * "collectionName-fieldName"
 */
public class IndexManager implements Closeable{
  private static Map<String, CollectionMan> collections;
  public IndexManager(){
    collections = new HashMap<String, CollectionMan>();
  }

  public void close() {
    for(CollectionMan man: collections.values()) {
      try {
        man.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean hasIndex(String collection, String indexName) {
    if(!collections.containsKey(collection)) {
      return false;
    }

    CollectionMan man = collections.get(collection);
    return man.hasIndex(indexName);
  }

  public String getIndexForField(String collection, String field) {
    if(!collections.containsKey(collection)) {
      return null;
    }
    CollectionMan man = collections.get(collection);
    return man.getIndexForField(field);
  }

}