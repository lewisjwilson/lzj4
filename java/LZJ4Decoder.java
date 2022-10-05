import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class LZJ4Decoder {
    
    public static Path PATH;
    public static long FILESIZE;
    public static byte[] data;
    
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
        data = new byte[(int)FILESIZE];
        
        try{
            // import lz4 data into bytearray
            data = Files.readAllBytes(PATH);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public static void main(String args[]) {
        
        boolean dataImport = importLZ4Data();
        System.out.println((char)data[5]);
    }
}
