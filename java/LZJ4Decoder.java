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
    public static ArrayList<byte[]> outputList = new ArrayList<>();
    public static int pos;
    
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
        
        // setting cursor position
        pos = 4;
        
        return Arrays.equals(magic, firstFour);
    }
    
    public static boolean endMarkerCheck() {
         byte[] end = {0, 0, 0, 0};
        // Final 4 bytes
        for(int i=0; i<4; i++){
            if(end[i] != dataList.get(0)[(int)FILESIZE-4+i]){return false;}
        }
        return true;
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
    
    public static void lz4Decode(){
        // pos has been set to 4 in magic number
        
        // Max assigned for offset in LZ4 specification (2 bytes)
        byte[] outData = new byte[65535];
        // For keeping track of the position to append to in outData
        int currentByte = 0;
         
        // -5: last 5 bytes should be uncompressed,
        // -4: Ending Marker should be 00 00 00 00
        while(pos<FILESIZE-5-4){
            String token = Integer.toHexString(dataList.get(0)[pos] & 0xFF);
            token = (token.length() < 2 ? "0" + token : token);
            int noOfSymbols = Integer.parseInt(token.substring(0,1), 16);
            int matchLen = Integer.parseInt(token.substring(1,2), 16) + 4;
             
            pos++;
            
            //Iterating through literals
            for(int i=0; i<noOfSymbols; i++){
                outData[currentByte] = dataList.get(0)[pos];
                //System.out.println(outData[currentByte]);
                currentByte++;
                pos++;
            }
            
            // Symbols match is next two bytes (little endian)
            String offsetStr = Integer.toHexString(dataList.get(0)[pos+1] & 0xFF);
            offsetStr += Integer.toHexString(dataList.get(0)[pos] & 0xFF);
            int offset = Integer.parseInt(offsetStr, 16);
            
            pos += 2;
            
            // Traversing the current decompressed data 'offset' bytes and copying matched data
            int copyFrom = currentByte - offset;
            
            // Append byte to outData in from every position between 
            // end of current outData and for matchLen position
            for(int i=copyFrom; i<copyFrom+matchLen; i++){
                outData[currentByte] = outData[i];
                currentByte++;
            }

        }
        
        // Appending the final 5 uncompressed bytes
        for(int i=0; i<5; i++){
           outData[currentByte] = dataList.get(0)[(int)FILESIZE-9+i];
           currentByte++;
        }
        
        //Verify the end marker matches 00 00 00 00
        boolean endMarker = endMarkerCheck();
        System.out.println("Ending Marker Verified?: " + endMarker);
        
        System.out.print("Decompressed data: ");
        for(int i=0; i<currentByte; i++){
           System.out.print((char)outData[i]);
        }
        
     }
    
    public static void main(String args[]) {
        
        // Verify the magic number is correct
        System.out.println("Magic Number Verified? " + magicNumber());
        
        // Import the data
        System.out.println("Data Import Sucessful? " + importLZ4Data());
        
        
        // Print LZ4 data to console
        System.out.print("LZ4 data: ");
        for(int i=0; i<FILESIZE; i++){
            System.out.print((char)dataList.get(0)[i]);
        }
        System.out.print("\n");
        
        // Decode the LZ4 data
        lz4Decode();
        
    }
}
