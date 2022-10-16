import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

class LZJ4Encoder extends FileOperations {

    private static String sourcePathStr = "../test_files/abbccabbcccabbaabcc.txt";
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

    // Initialize the number of new symbols used for token value in lz4 data block
    public static int newSymbolsCount = -1;

    // Cursor position initialization
    public static int pos = 0;

    // Getting the data in the window (search forward)
    public static byte[] lookAheadWindow;

    // Getting the data in the search buffer (search backward)
    public static byte[] searchBuffer;
    

    // Method to process the data in the search buffer (dealing with end cases)
    //HERE!! REFACTOR DOWNWARDS - return a byte array (subarray of the test data)
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

    private static void createDataBlock(int matchLength, byte[] symbols, ArrayList<Integer> matches,
            int newSymbolsCount) {

        // Initiate the lz4 data block
        LZ4DataBlock lz4block = new LZ4DataBlock();

        // Creating the token
        String hiToken = String.format("%4s", Integer.toBinaryString(newSymbolsCount)).replace(' ', '0');
        String loToken = String.format("%4s", Integer.toBinaryString(matchLength - 4)).replace(' ', '0');
        String token = hiToken + loToken;
        
        // Converting the binary string token to Hexadecimal
        int tokenDec = Integer.parseInt(token, 2);
        token = Integer.toHexString(tokenDec);

        // Processing the symbols
        byte[] symbolsArr = Arrays.copyOfRange(symbols, 0, newSymbolsCount);

        // Processing the offset
        String offset = String.format("%16s", Integer.toBinaryString(searchBuffer.length - matches.get(0))).replace(' ',
                '0');
        // Converting binary string offset to Hexadecimal
        int offsetDec = Integer.parseInt(offset, 2);
        String offsetHex = String.format("%4s", Integer.toHexString(offsetDec)).replace(' ', '0');
        String offsetByte1 = offsetHex.substring(2, 4);
        String offsetByte2 = offsetHex.substring(0, 2);

        lz4block.setToken(token);
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

    public static void dataTraverse(byte[] lookAheadWindow) {

        while (pos < FILESIZE - 5) {

            byte[] bestMatch = new byte[0];
            int matchLen = 0;
            byte[] subArr;
            searchBuffer = getBuffer();

            for (int c = 0; c < lookAheadWindow.length; c++) {
                // System.out.println("pos: " + pos);
                newSymbolsCount++;

                // Matches must be >= 4
                if (c < 4) {
                    pos++;
                    continue;
                }

                lookAheadWindow = getWindow();

                if (lookAheadWindow.length <= 0) {
                    System.out.println("End of window reached");
                    break;
                }

                searchBuffer = getBuffer();
                //System.out.println("buffer: " + Arrays.toString(searchBuffer));
                //System.out.println("window: " + Arrays.toString(lookAheadWindow));
                
                subArr = Arrays.copyOfRange(lookAheadWindow, 0, c); // get the current substring
                //System.out.println("subArr: " + Arrays.toString(subArr));

                ArrayList<Integer> matches = findMatches(searchBuffer, subArr);
                
                // If there are no matches, reset the subArr variable and continue
                if (matches.isEmpty()) {
                    subArr = new byte[0];
                    pos++;
                    continue;
                } else {
                    //System.out.println("Match found at index " + matches.get(0));
                    // If the length of the current best match is less than the length of the
                    // current substring
                    matchLen = subArr.length;
                    if (bestMatch.length < matchLen) {
                        // Replace the best match
                        bestMatch = subArr;
                        createDataBlock(matchLen, bestMatch, matches, newSymbolsCount);
                        //System.out.println("pos : " + pos + ", best match: " + Arrays.toString(bestMatch) +  ", match length: " + matchLen);
                        //System.out.println("matches indexes: " + matches.toString());
                        // Symbols since the previous match
                        // As matchLen number of symbols is appended after the literals in the token
                        // we minus matchLen number of new Symbols
                        newSymbolsCount = -matchLen;
                        break;
                    }
                }

            }

        }
        endOfData();
        //System.out.println("End of data reached!");

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

        dataTraverse(lookAheadWindow);

        FileOperations.closeOutputStream(outStream);

    }

}
