import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class LZJ4Encoder extends FileOperations {

    // Test Files
    private static String[] files = new String[]{"dk.txt", "a20", "abbccabbcccabbaabcc.txt"};

    // Initiate the lz4 data block
    private static LZ4DataBlock lz4block = new LZ4DataBlock();

    private static long fileSize;
    private static long posLimit;

    // List to store all bytes of source file
    protected static ArrayList<byte[]> dataList = new ArrayList<>();

    // List to store all bytes to be written
    protected static FileOutputStream outStream;
    protected static ArrayList<byte[]> compressedData = new ArrayList<>();

    // Cursor position initialization
    private static int pos = 1;

    private static void magicNumber(){
        byte[] magic = LZ4FrameFormat.magicNumber();
        writeData(magic);
    }

    // Writing data to file
    private static void writeData(byte[] dataToWrite){
        try {
            outStream.write(dataToWrite);
            compressedData.add(dataToWrite);
        } catch (IOException e) {
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
        if(matches.isEmpty()){return new int[0];}

        byte[] window = dataList.get(0);
        int litPos = pos;
        int[] bestMatch = new int[]{0, 0};

        // while there is more than one best match
        for (int[] match : matches) {
            int matchPos = match[0];

            while(litPos <= posLimit){
                if(window[litPos] == window[matchPos]){
                    litPos++;
                    matchPos++;
                    match[1]++;
                } else {
                    break;
                }
            }
            litPos = pos;
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
    

    private static void createDataBlock(byte[] literals, int startOfMatch, int matchLength) {

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
        writeData(dataBlock);
    }

    public static void dataTraverse(byte[] data) {

        int matchLen = 0;
        byte[] literalsToCopy = new byte[0];
        byte[] window;
        byte literalToCheck;

        // used in the case that a block was just created and a new block will now be created with no literals
        boolean blockJustCreated = false;

        // For the entirety of the data (minus the trailing 5 bytes)
        while(pos < posLimit) {

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
                pos++;
            } else {
                int matchStart = bestMatch[0];
                matchLen = bestMatch[1];
                
                if(matchLen < 4){
                    if(pos+1 > posLimit){
                        endOfData();
                    } else {
                        pos++;
                    }
                    continue;
                }

                // Create the LZ4 data block using information aquired
                createDataBlock(literalsToCopy, matchStart, matchLen);
                
                if(literalsToCopy.length <= 0){
                    pos = pos + matchLen + 1;
                } else {
                    pos = pos + matchLen;
                }
                
                // Reset variables
                literalsToCopy = new byte[0];
                blockJustCreated = true;
            }
        }
    }
    
    // Appending ending literals (in line with spec) and bytes (value 0)
    public static void endOfData() {

        System.out.println("pos; " + pos + " , FILESIZE; " + fileSize);
        long noOfEndBytes = fileSize - pos + 1;
        byte[] finalBytes = new byte[0];
        System.out.print("Final Uncompressed " + noOfEndBytes + " bytes: [");
        while (pos <= fileSize) {
            finalBytes = Arrays.copyOf(finalBytes, finalBytes.length + 1);
            finalBytes[finalBytes.length - 1] = dataList.get(0)[pos - 1];
            System.out.print(dataList.get(0)[pos - 1] + ", ");
            pos++;
        }
        System.out.print("]\n");
        writeData(finalBytes);

        byte[] endMarker = {0x00, 0x00, 0x00, 0x00};
        writeData(endMarker);

        System.out.println("End Marker: [00, 00, 00, 00]");        
    }

    public static void main(String[] args) throws FileNotFoundException {

        // for each test file
        for (String file : files) {
            
            String sourcePathStr = "/home/lewis/lzj4/test_binaries/" + file;
            // TODO: implement file selector. This is relevant for that functionality
            //if(sourcePathStr == null){
            //    System.out.println("No file selected!");
            //    System.exit(0);
            //}

            String[] pathPieces = sourcePathStr.split("/");
            String fileName = pathPieces[pathPieces.length-1];
            System.out.println("FILENAME: " + fileName);

            // Get filesize of the source file and raw data
            fileSize = FileOperations.getFileSize(sourcePathStr);
            posLimit = fileSize-5;
            dataList = FileOperations.importRawData(sourcePathStr);

            byte[] data = dataList.get(0);
            
            System.out.println("Uncompressed data: " + Arrays.toString(data));

            String outPathStr = "/home/lewis/lzj4/lz4_output_files/" + fileName + ".lz4";

            // Create a new file if not exists (else overwrite)
            FileOperations.createFile(outPathStr);

            // Create data stream for writing to file
            outStream = FileOperations.createOutputStream(outPathStr);

            // Writing data
            magicNumber();
            dataTraverse(data);
            endOfData();

            FileOperations.closeOutputStream(outStream);

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
