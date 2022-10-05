import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.InputStream;
import java.io.FileInputStream;

public class LZJ4Decoder {

    public static String pathStr = "abbccabbcccabbaabcc.lz4";
    public static Path PATH;
    public static long FILESIZE;
    public static ArrayList<byte[]> dataList = new ArrayList<>();
    
    
     public static boolean magicNumber(){
        byte[] magic = {0x04, 0x22, 0x4d, 0x18};
        byte[] firstFour = new byte[4];
        try{
            InputStream is = new FileInputStream(pathStr);
            // Reading first 4 bytes from file to be imported
            if (is.read(firstFour) != firstFour.length) {}
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Arrays.equals(magic, firstFour);
    }
    
    public static boolean importLZ4Data(){
        // file location
        PATH = Paths.get(pathStr);
        // size in bytes initialize
        FILESIZE = 0;
        
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
        
        // Verify the magic number is correct
        boolean magicNumber = magicNumber();
        System.out.println("Magic Number Verified? " + magicNumber);
        
        // Import the data
        boolean dataImport = importLZ4Data();
        System.out.println("Data Import Sucessful? " + dataImport);
        
        System.out.print("LZ4 data: ");
        for(int i=0; i<FILESIZE; i++){
            System.out.print((char)dataList.get(0)[i]);
        }
        System.out.print("\n");
    }
}
