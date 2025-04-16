import diskmanager.DiskFile;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String fileName = "storage/file1.db";
       
        DiskFile file = new DiskFile(fileName, 4096);
    
        System.out.println(file.getFileSize());
        System.out.println(file.getPageCnt());
        
        long pageID = 1;
        byte[] readData = file.readPage(pageID);
        System.out.println(new String(readData));
        byte[] data = {'m', 'o', 'h', 'a', 'm', 'm', 'a', 'd', '#', 'f', 'a', 't' , 'h', 'i'};
        file.writePage(pageID, data);
        readData = file.readPage(pageID);
        System.out.println(new String(readData));
        System.out.println(file.getFileSize());
        System.out.println(file.getPageCnt());
        file.close();
    }
}
