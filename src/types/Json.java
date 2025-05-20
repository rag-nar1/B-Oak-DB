package types;

public class Json implements Comparable<Json> {  
  private byte[] data;
  public int compareTo(Json rhs) {
    for(int i = 0; i < Types.JSON_SIZE; i ++) {
      if(data[i] < rhs.data[i]) {
        return -1;
      }

      if(data[i] > rhs.data[i]) {
        return 1;
      }
    }
    return 0;
  } 


}
