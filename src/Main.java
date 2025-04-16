import diskmanager.DiskManeger;
import diskmanager.DiskRequest;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, NullPointerException, ExecutionException {
        // Create storage directory if it doesn't exist
        File dir = new File("storage");
        if (!dir.exists()) {
            dir.mkdir();
        }
        
        String fileName = "storage/file1.db";
       
        DiskManeger DM = new DiskManeger();
        DM.open(fileName);

        // Now read the data back
        byte[] buffer = new byte[4096];
        DiskRequest read = new DiskRequest(fileName, 1, buffer, false);
        DM.pushRequest(read);
        CompletableFuture<Boolean> finish = read.getFuture();
        boolean done = finish.get();
        if (!done) {
            DM.close();
            throw new ExecutionException("Read failed", null);
        }

        // Print data as characters
        System.out.println("File content (as text):");
        System.out.println(new String(buffer));
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a new string to write to the file:");
        String newString = scanner.nextLine();

        buffer = new byte[4096];
        for (int i = 0; i < newString.length(); i++) {
            buffer[i] = (byte)newString.charAt(i);
        }

        DiskRequest write = new DiskRequest(fileName, 1, buffer, true);
        
        DM.pushRequest(write);
        finish = write.getFuture();
        done = finish.get();
        if (!done) {
            DM.close();
            scanner.close();
            throw new ExecutionException("Write failed", null);
        }

        DM.close();
        scanner.close();
    }
}
