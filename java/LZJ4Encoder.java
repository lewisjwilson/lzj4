import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Integer;


class LZJ4Encoder extends FileOperations {

    // Initiate the lz4 data block
    private static LZ4DataBlock lz4block = new LZ4DataBlock();

    private static String sourcePathStr = "/home/lewis/lzj4/test_files/abbccabbcccabbaabcc.txt";
    //private static String sourcePathStr = "/home/lewis/lzj4/test_files/a20";
    private static long FILESIZE;
    // List to store all bytes of source file
    public static ArrayList<byte[]> dataList = new ArrayList<>();

    // List to store all bytes to be written
    public static ArrayList<byte[]> outputList = new ArrayList<>();
    static FileOutputStream outStream;

    // Creating an output file
    static String outPathStr = "out.lz4";

    // Window buffer and search buffer variables
    static int lookAheadSize = 100000; // temporarily - to encode all data at once
    static int searchBufferSize = 100000; // temporarily - to encode all data at once

    // Cursor position initialization
    public static int pos = 0;

    // Getting the data in the window (search forward)
    public static byte[] lookAheadWindow;

    // Getting the data in the search buffer (search backward)
    public static byte[] searchBuffer;
    

    // Method to process the data in the search buffer (dealing with end cases)
    public static byte[] getBuffer() {
        if (pos - searchBufferSize < 0) {
            return Arrays.copyOfRange(dataList.get(0), 0, pos);
        } else {
            return Arrays.copyOfRange(dataList.get(0), pos - searchBufferSize, pos);
        }
    }

    // Method to process the data in the look-ahead window (dealing with end cases)
    public static byte[] getWindow() {
        if (pos + lookAheadSize > FILESIZE) {
            return Arrays.copyOfRange(dataList.get(0), pos, (int)FILESIZE);
        } else {
            return Arrays.copyOfRange(dataList.get(0), pos, pos + lookAheadSize);
        }
    }

    // Method to find the indexes of subarray matches in an array
    public static ArrayList<Integer> findMatches(byte[] mainArr, byte[] subArr) {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        for (int i = 0; i < mainArr.length; i++) {
            // Check if the first value matches
            if (mainArr[i] == subArr[0]) { 
                // Create temporary sub array
                byte[] temp = Arrays.copyOfRange(mainArr, i, (subArr.length + i));
                if (Arrays.equals(temp, subArr)) {
                    matches.add(i);
                }
            }
        }
        return matches;
    }

    private static void magicNumber(){
        byte[] magic = {0x04, 0x22, 0x4d, 0x18};
        try {
            outStream.write(magic);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.magicNumber()");
            e.printStackTrace();
        }
    }
    

    private static void createDataBlock(int matchLength, byte[] symbols, ArrayList<Integer> matches,
            int newSymbolsCount) {

        // Creating the token
        String hiToken = String.format("%4s", Integer.toBinaryString(newSymbolsCount)).replace(' ', '0');
        String loToken = String.format("%4s", Integer.toBinaryString(matchLength - 4)).replace(' ', '0');
        String token = hiToken + loToken;
        
        // Converting the binary string token to decimal
        System.out.println(" new symbols: " + newSymbolsCount);
        System.out.println(" match len: " + matchLength);

        int tokenDec = Integer.parseInt(token, 2);

        // The token value in decimal (will be converted to hex on writing)
        // System.out.println(tokenDec);

        // Processing the symbols
        byte[] symbolsArr = Arrays.copyOfRange(symbols, 0, newSymbolsCount);

        System.out.println(Arrays.toString(symbolsArr));

        // Processing the offset
        String offset = String.format("%16s", Integer.toBinaryString(searchBuffer.length - matches.get(0))).replace(' ',
                '0');
        // Converting binary string offset to Hexadecimal
        int offsetDec = Integer.parseInt(offset, 2);
        String offsetHex = String.format("%4s", Integer.toHexString(offsetDec)).replace(' ', '0');
        String offsetByte1 = offsetHex.substring(2, 4);
        String offsetByte2 = offsetHex.substring(0, 2);

        lz4block.setToken((byte)tokenDec);
        lz4block.setSymbols(symbolsArr);
        lz4block.setOffset(offsetByte1, offsetByte2);

        byte[] dataBlock = lz4block.createDataBlock();
        
        try {
            outStream.write(dataBlock);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.createDataBlock()");
            e.printStackTrace();
        }

    }

    private static boolean lastFiveBytes(){
        return pos >= FILESIZE - 5;
    }

    public static void dataTraverse(byte[] lookAheadWindow) {

        while (!lastFiveBytes()) {
            byte[] literals = new byte[0];
            int matchLen = 0;
            byte[] potentialMatch;
            searchBuffer = getBuffer();

            int lastMatchFoundAtPos = 0;

            int c = 0;

            while(c < lookAheadWindow.length) {
                //System.out.println("pos: " + pos);

                lookAheadWindow = getWindow();

                if (lookAheadWindow.length <= 0) {
                    System.out.println("End of window reached");
                    break;
                }

                searchBuffer = getBuffer();
                // System.out.println("buffer: " + Arrays.toString(searchBuffer));
                // System.out.println("window: " + Arrays.toString(lookAheadWindow));
                
                potentialMatch = Arrays.copyOfRange(lookAheadWindow, 0, c); // get the current substring
                // System.out.println("c: " + c);

                // In the case that the first literal is checked after a data block has been created
                // Required so that current literal can be searched for in buffer
                if(potentialMatch.length <= 0 && searchBuffer.length > 0){
                    potentialMatch = Arrays.copyOfRange(lookAheadWindow, 0, c+1); // get the current substring
                } else if (potentialMatch.length > searchBuffer.length){
                    pos++;
                    searchBuffer = getBuffer();
                }

                // System.out.println(lookAheadWindow.length);
                //System.out.println("buffer: " + Arrays.toString(searchBuffer));
                //System.out.println("potentialMatch: " + Arrays.toString(potentialMatch));

                c++;

                ArrayList<Integer> matches = findMatches(searchBuffer, potentialMatch);
                // System.out.println("matches: " + matches);
                // System.out.println("litlen: " + literalsLength);
                
                // If there are no matches, reset the subArr variable and continue
                if (matches.isEmpty()) {
                    potentialMatch = new byte[0];
                    pos++;
                    continue;
                } else {

                    literals = Arrays.copyOfRange(dataList.get(0), lastMatchFoundAtPos, pos);
                    
                    lastMatchFoundAtPos = pos;
                    matchLen = potentialMatch.length;
                    // System.out.println("Match found at index " + matches.get(0));
                    // If the length of the current best match is less than the length of the
                    // current substring

                    System.out.println("lits: "  + Arrays.toString(literals));
                    System.out.println("matchlen: "  + matchLen);

                    if(matchLen < 4){
                        continue;
                    }
                    
                    if (literals.length <= matchLen) {

                        // Replace the best match
                        

                        createDataBlock(matchLen, literals, matches, literals.length);
                        // System.out.println("pos : " + pos + ", best match: " + Arrays.toString(bestMatch) +  ", match length: " + matchLen);
                        // System.out.println("matches indexes: " + matches.toString());

                        // This if statement changes the position of the cursor
                        // and also acounts for if no symbols were copied to the previous block
                        if(lz4block.getSymbols().length <= 0){
                            pos = pos + matchLen + 1;
                        } else {
                            pos = pos + matchLen;
                        }
                        
                        System.out.println("pos: " + pos + " , matchlen: " + matchLen + " , symbols: " + lz4block.getSymbols().length);
                        
                        break;
                    }
                }

            }

        }
    }
    
    // Appending 5 ending literals (in line with spec) and bytes (value 0)
    public static void endOfData() {
        try {
            int posMax = pos + 5;
            System.out.print("Final 5 bytes: [");
            while (pos < posMax) {
                outStream.write((byte) dataList.get(0)[pos - 1]);
                System.out.print(dataList.get(0)[pos - 1] + ", ");
                pos++;
            }
            System.out.print("]\n");
            outStream.write((byte) 0);
            outStream.write((byte) 0);
            outStream.write((byte) 0);
            outStream.write((byte) 0);
            System.out.println("End Marker: [00, 00, 00, 00]");
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.endOfData()");
            e.printStackTrace();
        }
        
    }

    public static void main(String[] args) throws FileNotFoundException {

        // Get filesize of the source file and raw data
        FILESIZE = FileOperations.getFileSize(sourcePathStr);
        dataList = FileOperations.importRawData(sourcePathStr);

        lookAheadWindow = getWindow();
        searchBuffer = getBuffer();
        
        System.out.println("Uncompressed data: " + Arrays.toString(dataList.get(0)));

        // Create a new file if not exists (else overwrite)
        FileOperations.createFile(outPathStr);

        // Create data stream for writing to file
        outStream = FileOperations.createOutputStream(outPathStr);

        // Writing data
        magicNumber();
        dataTraverse(lookAheadWindow);
        endOfData();
        System.out.println("End of data reached!");

        FileOperations.closeOutputStream(outStream);

        System.out.println("File \"" + outPathStr + "\" successfully written.");

    }

}