import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
 

public class LZJ4Decoder extends FileOperations {

    private static String fileName = "a20";
    private static String pathStr = "/home/lewis/lzj4/lz4_output_files/" + fileName + ".lz4";
    private static long fileSize;
    private static ArrayList<byte[]> dataList = new ArrayList<>();
    private static int pos;

    // List to store all bytes to be written
    private static FileOutputStream outStream;
    
    public static boolean magicNumber(){
        byte[] magic = {0x04, 0x22, 0x4d, 0x18};
        byte[] firstFour = new byte[4];
        try (InputStream is = new FileInputStream(pathStr)){
            // Reading first 4 bytes from file to be imported
            if (is.read(firstFour) != firstFour.length) {}
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
            if(end[i] != dataList.get(0)[(int)fileSize-4+i]){return false;}
        }
        return true;
    }
    
    public static boolean importLZ4Data(){
        // file location
        Path path = Paths.get(pathStr);
        // size in bytes initialize
        fileSize = 0;
        
        try{
            // get filesize of selected lz4 file
            fileSize = Files.size(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        // initialize data bytearray with size of filesize
        // THIS ASSUMES FILESIZE IS WITHIN THE BOUNDS OF AN INT.
        // TODO: if filesize > Integer.Max split data into multiple
        // byte[] arrays and append to bytelist in order.
        byte[] dataArray = new byte[(int)fileSize];
        
        try{
            // import lz4 data into bytearray
            dataArray = Files.readAllBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        dataList.add(dataArray);
        return true;
    }

    public static void writeData(byte[] dataToWrite){
        try {
            outStream.write(dataToWrite);
        } catch (IOException e) {
            System.out.println("writeData()");
            e.printStackTrace();
        }
    }
    
    public static void lz4Decode(){
        // pos has been set to 4 in magic number
        
        // Max assigned for offset in LZ4 specification (2 bytes)
        byte[] outData = new byte[65535];
        // For keeping track of the position to append to in outData
        int currentByte = 0;
         
        // -5: last 5 bytes should be uncompressed,
        // -4: Ending Marker should be 00 00 00 00
        while(pos<fileSize-5-4){
            String token = Integer.toHexString(dataList.get(0)[pos] & 0xFF);
            // value of the token in hex
            token = (token.length() < 2 ? "0" + token : token);

            // 16 symbolises hex-->decimal
            int noOfSymbols = Integer.parseInt(token.substring(0,1), 16);
            int matchLen = Integer.parseInt(token.substring(1,2), 16) + 4;

            System.out.println("pos: " + pos + " byte: " + token + " " + noOfSymbols);
             
            pos++;
            
            //Iterating through literals
            for(int i=0; i<noOfSymbols; i++){
                outData[currentByte] = dataList.get(0)[pos];
                System.out.println("pos: " + pos + " byte: " + outData[currentByte]);
                currentByte++;
                pos++;
            }
            
            // Symbols match is next two bytes (little endian)
            String offsetStr = Integer.toHexString(dataList.get(0)[pos+1] & 0xFF);
            offsetStr += Integer.toHexString(dataList.get(0)[pos] & 0xFF);
            int offset = Integer.parseInt(offsetStr, 16);

            System.out.println("pos: " + pos + " byte: " + offsetStr);
            
            pos += 2;
            
            // Traversing the current decompressed data 'offset' bytes and copying matched data
            int copyFrom = currentByte - offset;
            if(copyFrom<0){
                int dataPos = pos-2;
                throw new Error("Offset exceeds length of the current decompressed data. " +
                "Data Position: Byte " + dataPos + ", Offset (Hex, Little Endian): " + offsetStr);
            }
            
            // Append byte to outData in from every position between 
            // end of current outData and for matchLen position
            byte[] writeBytes = new byte[0];
            // create byte array to write to file
            for(int i=copyFrom; i<copyFrom+matchLen; i++){
                outData[currentByte] = outData[i];
                writeBytes = Arrays.copyOf(writeBytes, writeBytes.length + 1);
                writeBytes[writeBytes.length - 1] = outData[currentByte];
                currentByte++;
            }
            // write the data
            writeData(writeBytes);

        }

        byte[] writeBytes = new byte[0];        
        // Appending the final 5 uncompressed bytes
        for(int i=0; i<5; i++){
           outData[currentByte] = dataList.get(0)[(int)fileSize-9+i];
           writeBytes = Arrays.copyOf(writeBytes, writeBytes.length + 1);
           writeBytes[writeBytes.length - 1] = outData[currentByte];
           currentByte++;
        }
        // write the data
        writeData(writeBytes);
        
        //Verify the end marker matches 00 00 00 00
        if(!endMarkerCheck()){
            throw new Error("Ending Marker Incorrect (Should be {0x00 0x00 0x00 0x00})");
        }
        System.out.println("Ending Marker Verified.");
        
        
        System.out.print("Decompressed data: ");
        for(int i=0; i<currentByte; i++){
           System.out.print((char)outData[i]);
        }
        
     }
    
    public static void main(String[] args) throws FileNotFoundException {
        
        // Verify the magic number is correct
        if(!magicNumber()){
            throw new Error("Not a valid LZ4 file.");
        }
        System.out.println("Magic Number Verified.");
        
        // Import the data
        if(!importLZ4Data()){
            throw new Error("LZ4 data could not be read.");
        }
        System.out.println("LZ4 Data Read Sucessful.");
        
        // Print LZ4 data to console
        System.out.print("LZ4 data: [ ");
        for(int i=0; i<fileSize; i++){
            System.out.print((int)dataList.get(0)[i] + ", ");
        }
        System.out.print("]\n");

        String outPathStr = "/home/lewis/lzj4/decoder_output_files/" + fileName;
        // Create a new file if not exists (else overwrite)
        FileOperations.createFile(outPathStr);

        // Create data stream for writing to file
        outStream = FileOperations.createOutputStream(outPathStr);
                
        // Decode the LZ4 data
        lz4Decode();

        FileOperations.closeOutputStream(outStream);
        
    }
}
