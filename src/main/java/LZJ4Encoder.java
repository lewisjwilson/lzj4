import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class LZJ4Encoder extends FileOperations {

    // Test Files
    static String[] files = new String[]{"dk.txt", "a20", "abbccabbcccabbaabcc.txt"};

    // Initiate the lz4 data block
    private static LZ4DataBlock lz4block = new LZ4DataBlock();

    public static String sourcePathStr;
    public static String FILENAME;
    public static long FILESIZE;
    public static long POS_LIMIT;

    // List to store all bytes of source file
    public static ArrayList<byte[]> dataList = new ArrayList<>();

    // List to store all bytes to be written
    public static FileOutputStream outStream;
    public static ArrayList<byte[]> compressedData = new ArrayList<>();

    // Cursor position initialization
    public static int pos = 1;

    private static void magicNumber(){
        byte[] magic = {0x04, 0x22, 0x4d, 0x18};
        try {
            outStream.write(magic);
            compressedData.add(magic);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.magicNumber()");
            e.printStackTrace();
        }
    }


    // Find the best matches avaliable from the current matches array
    public static int[] findBestMatches(byte[] array, byte literal) {
        // Get position of match of item in array
        // Output : ArrayList<{startOfMatch, lengthOfMatch}>    
        ArrayList<int[]> matches = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            // Check if the first value matches
            if (array[i] == literal) { 
                matches.add(new int[]{i, 1});
            }
        }

        // if the first literal does not match any literals in the window
        if(matches.size() <= 0){return new int[0];}

        byte[] window = dataList.get(0);
        int lit_pos = pos;
        int[] bestMatch = new int[]{0, 0};

        // while there is more than one best match
        for (int[] match : matches) {
            int match_pos = match[0];

            while(lit_pos <= POS_LIMIT){
                if(window[lit_pos] == window[match_pos]){
                    lit_pos++;
                    match_pos++;
                    match[1]++;
                } else {
                    break;
                }
            }
            lit_pos = pos;
        }

        int best = 0;

        for (int[] match : matches) {
            if(match[1]-1 > best){
                best = match[1]-1;
                bestMatch[0] = match[0];
                bestMatch[1] = best;
            }
        }

        return bestMatch;
    }
    
    private static byte[][] tokenPlusHandler(String hiToken, String loToken){
        byte[] hiTokenPlus = new byte[0];
        byte[] loTokenPlus = new byte[0];
        int hiTokenDec = Integer.parseInt(hiToken, 2) - 15;
        int loTokenDec = Integer.parseInt(loToken, 2) - 15;
        while(hiTokenDec > 0){
            hiTokenPlus = Arrays.copyOf(hiTokenPlus, hiTokenPlus.length + 1);
            if(hiTokenDec <= 255){
                hiTokenPlus[hiTokenPlus.length - 1] = (byte)hiTokenDec;
                // A value of 255 (FF) symbolises the addition of another byte
                if(hiTokenDec == 255){
                    hiTokenPlus = Arrays.copyOf(hiTokenPlus, hiTokenPlus.length + 1);
                    hiTokenPlus[hiTokenPlus.length - 1] = (byte)0;
                }
                break;
            }
            hiTokenPlus[hiTokenPlus.length - 1] = (byte)255;
            hiTokenDec -= 255;
        }
        
        while(loTokenDec > 0){
            loTokenPlus = Arrays.copyOf(loTokenPlus, loTokenPlus.length + 1);
            if(loTokenDec <= 255){
                loTokenPlus[loTokenPlus.length - 1] = (byte)loTokenDec;
                // A value of 255 (FF) symbolises the addition of another byte
                if(loTokenDec == 255){
                    loTokenPlus = Arrays.copyOf(loTokenPlus, loTokenPlus.length + 1);
                    loTokenPlus[loTokenPlus.length - 1] = (byte)0;
                }
                break;
            }
            loTokenPlus[loTokenPlus.length - 1] = (byte)255;
            loTokenDec -= 255;
        }
        return new byte[][]{hiTokenPlus, loTokenPlus};
    }
    

    private static void createDataBlock(byte[] literals, int startOfLiterals, int startOfMatch, int matchLength) {

        // Creating the token
        String hiToken = String.format("%4s", Integer.toBinaryString(literals.length)).replace(' ', '0');
        String loToken = String.format("%4s", Integer.toBinaryString(matchLength - 4)).replace(' ', '0');
        String token = hiToken + loToken;
        int tokenDec = Integer.parseInt(token, 2);
        
        // Handling the case where the hi or lo token exceeds 15 (F)
        byte[][] hiLoTokenPlus = tokenPlusHandler(hiToken, loToken);

        // Processing the offset
        String offset = "";
        if(matchLength > pos){
            offset = String.format("%16s", Integer.toBinaryString(pos)).replace(' ',
            '0');
        } else {
            offset = String.format("%16s", Integer.toBinaryString(pos - startOfMatch)).replace(' ',
                '0');
        }
        // Converting binary string offset to Hexadecimal
        int offsetDec = Integer.parseInt(offset, 2);
        String offsetHex = String.format("%4s", Integer.toHexString(offsetDec)).replace(' ', '0');
        String offsetByte1 = offsetHex.substring(2, 4);
        String offsetByte2 = offsetHex.substring(0, 2);

        // Setting values for LZ4 block to be created
        lz4block.setToken((byte)tokenDec);
        lz4block.setHiTokenPlus(hiLoTokenPlus[0]);
        lz4block.setSymbols(literals);
        lz4block.setOffset(offsetByte1, offsetByte2);
        lz4block.setHiTokenPlus(hiLoTokenPlus[1]);
        
        // Creating and writing the LZ4 block
        byte[] dataBlock = lz4block.createDataBlock();
        try {
            outStream.write(dataBlock);
            compressedData.add(dataBlock);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.createDataBlock()");
            e.printStackTrace();
        }

    }

    public static void dataTraverse(byte[] data) {

        int matchLen = 0;
        byte[] literalsToCopy = new byte[0];
        byte[] window;
        byte literalToCheck;
        int startOfLiterals = 0;

        // used in the case that a block was just created and a new block will now be created with no literals
        boolean blockJustCreated = false;

        // For the entirety of the data (minus the trailing 5 bytes)
        while(pos < POS_LIMIT) {

            window = Arrays.copyOfRange(data, 0, pos);      
            literalToCheck = data[pos];
                                    
            int[] bestMatch = findBestMatches(window, literalToCheck);

            // this occurs ONLY when a block was just created and no new literals will be appended
            if(!blockJustCreated){
                literalsToCopy = Arrays.copyOf(literalsToCopy, literalsToCopy.length + 1);
                literalsToCopy[literalsToCopy.length - 1] = data[pos-1];
            }

            // If there are no matches, increase pos and continue
            if (bestMatch.length <= 0) {
                //System.out.println("No matches");
                pos++;
                continue;
            } else {
                //System.out.println("Best match (fromPos, matchLen): " + Arrays.toString(bestMatch));
                int matchStart = bestMatch[0];
                matchLen = bestMatch[1];
                
                if(matchLen < 4){
                    if(pos+1 > POS_LIMIT){
                        endOfData();
                    } else {
                        pos++;
                    }
                    continue;
                }

                // Create the LZ4 data block using information aquired
                createDataBlock(literalsToCopy, startOfLiterals, matchStart, matchLen);
                
                if(literalsToCopy.length <= 0){
                    pos = pos + matchLen + 1;
                } else {
                    pos = pos + matchLen;
                }
                //System.out.println("pos: " + pos + " , matchlen: " + matchLen + " , literals: " + lz4block.getSymbols().length);
                
                // Reset variables
                startOfLiterals = pos;
                matchLen = 0;
                literalsToCopy = new byte[0];
                blockJustCreated = true;
            }
        }
    }
    
    // Appending ending literals (in line with spec) and bytes (value 0)
    public static void endOfData() {
        try {
            System.out.println("pos; " + pos + " , FILESIZE; " + FILESIZE);
            long noOfEndBytes = FILESIZE - pos + 1;
            System.out.print("Final Uncompressed " + noOfEndBytes + " bytes: [");
            while (pos <= FILESIZE) {
                outStream.write((byte) dataList.get(0)[pos - 1]);
                System.out.print(dataList.get(0)[pos - 1] + ", ");
                pos++;
            }
            System.out.print("]\n");

            byte[] endMarker = {0x00, 0x00, 0x00, 0x00};
            try {
                outStream.write(endMarker);
            } catch (IOException e) {
                System.out.println("LZJ4Encoder.endOfData()");
                e.printStackTrace();
            }

            System.out.println("End Marker: [00, 00, 00, 00]");
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.endOfData()");
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args) throws FileNotFoundException {

        // for each test file
        for (String file : files) {
            
            //sourcePathStr = FileOperations.selectFile();         
            sourcePathStr = "test_binaries/" + file;
            if(sourcePathStr == null){
                System.out.println("No file selected!");
                System.exit(0);
            }

            String[] pathPieces = sourcePathStr.split("/");
            FILENAME = pathPieces[pathPieces.length-1];
            System.out.println("FILENAME: " + FILENAME);

            // Get filesize of the source file and raw data
            FILESIZE = FileOperations.getFileSize(sourcePathStr);
            POS_LIMIT = FILESIZE-5;
            dataList = FileOperations.importRawData(sourcePathStr);

            byte[] data = dataList.get(0);
            
            System.out.println("Uncompressed data: " + Arrays.toString(data));

            //String outPathStr = "/home/lewis/lzj4/lz4_output_files/" + FILENAME + ".lz4";

            String outPathStr = "out.lz4";
            // Create a new file if not exists (else overwrite)
            FileOperations.createFile(outPathStr);

            // Create data stream for writing to file
            outStream = FileOperations.createOutputStream(outPathStr);

            // Writing data
            magicNumber();
            dataTraverse(data);
            endOfData();

            FileOperations.closeOutputStream(outStream);
            //System.out.println("File \"" + outPathStr + "\" successfully written.");

            // Printing to verify compressed data in terminal
            System.out.print("Compressed Data: ");
            compressedData = FileOperations.importRawData(outPathStr);
            System.out.print(Arrays.toString(compressedData.get(0)));
            System.out.print(" (len: " + compressedData.get(0).length + ")\n\n");

            resetPos();
        }
       
    }

    public static void resetPos(){
        pos = 1;
    }

}
