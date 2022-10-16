import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileOperations {

    public static void createFile(String target){

        File outFile = new File(target);
        if (!outFile.exists()) {
            try {
                outFile.createNewFile();
            } catch (Exception e) {
                System.out.println("LZJ4Encoder.main()");
                e.printStackTrace();
            }
        }

    }

    public static FileOutputStream createOutputStream(String target) throws FileNotFoundException{
        return new FileOutputStream(target, false);
    }

    public static void closeOutputStream(FileOutputStream stream) throws FileNotFoundException{
        try {
            stream.close();
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.main()");
            e.printStackTrace();
        }
    }

    public static ArrayList<byte[]> importRawData(String source){
        
        // file location
        Path PATH = Paths.get(source);

        // Get the size of the file in bytes
        long FILESIZE = getFileSize(source);

        ArrayList<byte[]> dataList = new ArrayList<>();

        // initialize data bytearray with size of filesize
        // THIS ASSUMES FILESIZE IS WITHIN THE BOUNDS OF AN INT.
        // TODO: if filesize > Integer.Max split data into multiple
        // byte[] arrays and append to bytelist in order.
        byte[] dataArray = new byte[(int)FILESIZE];

        
        try{
            // import data into bytearray
            dataArray = Files.readAllBytes(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        dataList.add(dataArray);

        return dataList;
    }
    
    public static long getFileSize(String source){

        // file location
        Path PATH = Paths.get(source);

         // size in bytes initialize
         long FILESIZE = 0;
        
         try{
             // Read input filesize
             FILESIZE = Files.size(PATH);
         } catch (Exception e) {
             e.printStackTrace();
         }
         return FILESIZE;
    }

}
