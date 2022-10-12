import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

class LZJ4Encoder {

    public static String pathStr = "test_files/abbccabbcccabbaabcc.txt";
    public static Path PATH;
    public static long FILESIZE;
    public static ArrayList<byte[]> dataList = new ArrayList<>();
    public static ArrayList<byte[]> outputList = new ArrayList<>();

    // Creating an output file
    static String path = "out.lz4";
    static File outFile = new File(path);
    static FileOutputStream outStream;

    // Data variables - TODO: to be made into an import instead
    static byte[] testData = "abbccabbcccabbaabcc".getBytes();

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

    public static void importData(){
        // file location
        PATH = Paths.get(pathStr);
        
        // size in bytes initialize
        FILESIZE = 0;
        
        try{
            // Read input filesize
            FILESIZE = Files.size(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
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
    }

    // Method to process the data in the search buffer (dealing with end cases)
    //HERE!! REFACTOR DOWNWARDS - return a byte array (subarray of the test data)
    public static byte[] getBuffer() {
        if (pos - searchBufferSize < 0) {
            return Arrays.copyOfRange(testData, 0, pos);
        } else {
            return Arrays.copyOfRange(testData, pos - searchBufferSize, pos);
        }
    }

    // Method to process the data in the look-ahead window (dealing with end cases)
    public static byte[] getWindow() {
        if (pos + lookAheadSize > FILESIZE) {
            return Arrays.copyOfRange(testData, pos, (int)FILESIZE);
        } else {
            return Arrays.copyOfRange(testData, pos, pos + lookAheadSize);
        }
    }

    // Method to find the indexes of substring matches in a string
    public static ArrayList<Integer> findMatches(byte[] mainArr, byte[] subArr) {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        // TODO: implement a matching system for byte arrays
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
            while (pos < posMax) {
                outStream.write((byte) testData[pos - 1]);
                pos++;
            }
            outStream.write((byte) 0);
            outStream.write((byte) 0);
            outStream.write((byte) 0);
            outStream.write((byte) 0);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.endOfData()");
            e.printStackTrace();
        }
    }

    public static void dataTraverse(byte[] lookAheadWindow) {

        while (pos < FILESIZE - 5) {

            byte[] bestMatch = new byte[100000];
            int matchLen = 0;
            byte[] subArr = new byte[100000];
            searchBuffer = getBuffer();

            // TODO: lookAheadWindow.length will output 100000, so need to fix this.
            for (int c = 0; c < lookAheadWindow.length; c++) {
                // System.out.println("pos: " + pos);
                newSymbolsCount++;

                // Matches must be >= 4
                if (c < 4) {
                    pos++;
                    continue;
                }

                lookAheadWindow = getWindow();

                // TODO: lookAheadWindow.length will output 100000, so need to fix this.
                if (lookAheadWindow.length <= 0) {
                    System.out.println("End of window reached");
                    break;
                }

                searchBuffer = getBuffer();
                //System.out.println("buffer: " + buffer);
                //System.out.println("window: " + window);
                
               subArr = Arrays.copyOfRange(lookAheadWindow, 0, c); // get the current substring
                //System.out.println("subStr: " + subStr);

                ArrayList<Integer> matches = findMatches(searchBuffer, subArr);

                // If there are no matches, reset the subStr variable and continue
                if (matches.isEmpty()) {
                    subArr = new byte[100000];;
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
                        System.out.println("pos : " + pos + ", best match: " + bestMatch +  ", match length: " + matchLen);
                        System.out.println("matches indexes: " + matches.toString());
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

    public static void main(String[] args) {

        importData();
        lookAheadWindow = getWindow();
        searchBuffer = getBuffer();
        
        System.out.println("Uncompressed data: " + Arrays.toString(dataList.get(0)));

        if (!outFile.exists()) {
            try {
                outFile.createNewFile();
            } catch (Exception e) {
                System.out.println("LZJ4Encoder.main()");
                e.printStackTrace();
            }
        }

        try {
            outStream = new FileOutputStream(path, true);
        } catch (FileNotFoundException e) {
            System.out.println("LZJ4Encoder.main()");
            e.printStackTrace();
        }

        dataTraverse(lookAheadWindow);

        try {
            outStream.close();
            System.out.println("File \"" + path + "\" successfully written.");
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.main()");
            e.printStackTrace();
        }

    }

}
