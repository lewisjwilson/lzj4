import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;;

public class LZJ4Decoder {
    
    public static Path PATH;
    public static long FILESIZE;
    public static ArrayList<byte[]> dataList = new ArrayList<>();
    
    public static boolean importLZ4Data(){
        
        // file location
        Path PATH = Paths.get("abbccabbcccabbaabcc.lz4");
        
        // size in bytes initialize
        long FILESIZE = 0;
        
        try{
            // get filesize of selected lz4 file
            FILESIZE = Files.size(PATH);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        // initialize data bytearray with size of filesize
        // THIS ASSUMES FILESIZE IS WITHIN THE BOUNDS OF AN INT.
        // TODO: if filesize > Integer.Max split data into multiple
        // byte[] arrays and append to bytelist in order.
        byte[] dataArray = new byte[(int)FILESIZE];
        
        try{
            // import lz4 data into bytearray
            dataArray = Files.readAllBytes(PATH);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        dataList.add(dataArray);
        return true;
    }
    
    public static void main(String args[]) {
        
        boolean dataImport = importLZ4Data();
        System.out.println((char)dataList.get(0)[5]);
    }
}
