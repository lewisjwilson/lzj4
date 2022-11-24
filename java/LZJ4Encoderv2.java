import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Integer;


class LZJ4Encoderv2 extends FileOperations {

    // Initiate the lz4 data block
    private static LZ4DataBlock lz4block = new LZ4DataBlock();

    // private static String sourcePathStr = "test_files/abbccabbcccabbaabcc.txt";
    private static String sourcePathStr = "test_files/a20";
    private static long FILESIZE;
    // List to store all bytes of source file
    public static ArrayList<byte[]> dataList = new ArrayList<>();

    // List to store all bytes to be written
    public static ArrayList<byte[]> outputList = new ArrayList<>();
    static FileOutputStream outStream;

    // Creating an output file
    static String outPathStr = "out.lz4";

    // Cursor position initialization
    public static int pos = 1;


    private static void magicNumber(){
        byte[] magic = {0x04, 0x22, 0x4d, 0x18};
        try {
            outStream.write(magic);
        } catch (IOException e) {
            System.out.println("LZJ4Encoder.magicNumber()");
            e.printStackTrace();
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

    private static void createDataBlock(byte[] literals, int startOfLiterals, ArrayList<Integer> matches,
            int matchLength, int currentPos) {

        // Creating the token
        String hiToken = String.format("%4s", Integer.toBinaryString(literals.length)).replace(' ', '0');
        String loToken = String.format("%4s", Integer.toBinaryString(matchLength - 4)).replace(' ', '0');
        String token = hiToken + loToken;
        
        // Converting the binary string token to decimal
        System.out.println(" new symbols: " + literals.length);
        System.out.println(" match len: " + matchLength);

        int tokenDec = Integer.parseInt(token, 2);

        // The token value in decimal (will be converted to hex on writing)
        // System.out.println(tokenDec);

        // Processing the offset
        String offset = String.format("%16s", Integer.toBinaryString(currentPos - matchLength)).replace(' ',
                '0');
        // Converting binary string offset to Hexadecimal
        int offsetDec = Integer.parseInt(offset, 2);
        String offsetHex = String.format("%4s", Integer.toHexString(offsetDec)).replace(' ', '0');
        String offsetByte1 = offsetHex.substring(2, 4);
        String offsetByte2 = offsetHex.substring(0, 2);


        lz4block.setToken((byte)tokenDec);
        lz4block.setSymbols(literals);
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

    public static void dataTraverse(byte[] data) {

        byte[] literals = new byte[0];
        int matchLen = 0;
        byte[] potentialMatch;

        // For the entirety of the data (minus the trailing 5 bytes)
        for (int i = 0; i < FILESIZE-5; i++) {

            int startOfLiterals = pos;
            
            literals = Arrays.copyOf(literals, literals.length + 1); // adjusting the size of the literals array
            literals[literals.length - 1] = data[pos]; // append literal
                        
            potentialMatch = Arrays.copyOfRange(data, 0, pos); // get data on LHS from current pos

            System.out.println("\npos: " + pos);
            System.out.println("potentialMatch: " + Arrays.toString(potentialMatch));
            System.out.println("literals: " + Arrays.toString(literals));

            ArrayList<Integer> matches = findMatches(potentialMatch, literals);

            System.out.println("matches (pos): " + matches);


            // If there are no matches, reset the potentialMatch variable and continue
            if (matches.isEmpty()) {

                System.out.println("here");
                literals = new byte[0];
                pos++;
                continue;

            } else {

                // this variable takes over in case that multiple matches are found past pos
                int window_counter = pos;

                while(matches.size() > 0) {
                    window_counter++;
                    if(window_counter >= FILESIZE - 5){
                        break;
                    }
                    System.out.println("last 5 bytes? " + lastFiveBytes());
                    
                    //literals = Arrays.copyOf(literals, literals.length + 1); // adjusting the size of the literals array
                    //literals[literals.length - 1] = data[pos]; // append literal

                    potentialMatch = Arrays.copyOfRange(data, 0, window_counter); // get data on LHS from current pos

                    System.out.println("\npos: " + pos);
                    System.out.println("potentialMatch: " + Arrays.toString(potentialMatch));
                    System.out.println("literals: " + Arrays.toString(literals));

                    matches = findMatches(potentialMatch, literals);

                    System.out.println("matches (pos): " + matches);

                }
                                  
                matchLen = potentialMatch.length;
                
                if(matchLen < 4){
                    pos++;
                    continue;
                }

                if (literals.length <= matchLen) {

                    createDataBlock(literals, startOfLiterals, matches, matchLen, window_counter);


                    if(lz4block.getSymbols().length <= 0){
                        pos = pos + matchLen + 1;
                    } else {
                        pos = pos + matchLen;
                    }
                    
                    System.out.println("pos: " + pos + " , matchlen: " + matchLen + " , literals: " + lz4block.getSymbols().length);
                    
                    matchLen = 0;

                    break;
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

        // Get filesize of the source file and raw data
        FILESIZE = FileOperations.getFileSize(sourcePathStr);
        dataList = FileOperations.importRawData(sourcePathStr);

        byte[] data = dataList.get(0);
        
        System.out.println("Uncompressed data: " + Arrays.toString(data));

        // Create a new file if not exists (else overwrite)
        FileOperations.createFile(outPathStr);

        // Create data stream for writing to file
        outStream = FileOperations.createOutputStream(outPathStr);

        // Writing data
        magicNumber();
        dataTraverse(data);
        endOfData();
        System.out.println("End of data reached!");

        FileOperations.closeOutputStream(outStream);

        System.out.println("File \"" + outPathStr + "\" successfully written.");

    }

}