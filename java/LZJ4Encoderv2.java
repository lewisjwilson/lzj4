import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Integer;


class LZJ4Encoderv2 extends FileOperations {

    // Initiate the lz4 data block
    private static LZ4DataBlock lz4block = new LZ4DataBlock();

    //private static String sourcePathStr = "test_files/abbccabbcccabbaabcc.txt";
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

    // Get position of match of item in array
    // Output : ArrayList<{startOfMatch, lengthOfMatch}>
    public static ArrayList<int[]> findFirstMatch(byte[] array, byte literal) {
        ArrayList<int[]> matches = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            // Check if the first value matches
            if (array[i] == literal) { 
                matches.add(new int[]{i, 1});
            }
        }
        return matches;
    }

    // Find the best matches avaliable from the current matches array
    public static int[] findBestMatches(ArrayList<int[]> matches, int checkFromPosition) {
        byte[] window = dataList.get(0);
        int lit_pos = checkFromPosition;
        int[] bestMatch = new int[]{0, 0};
     
        // while there is more than one best match
        for (int[] match : matches) {
            int match_pos = match[0];
            int best = 1;
            
            while(lit_pos < FILESIZE - 5){
                if(window[lit_pos] == window[match_pos]){
                    System.out.println("HERE: " + (char)window[match_pos]);
                    lit_pos++;
                    match_pos++;
                    best++;
                } else { 
                    break;
                }
            }
            if(best > bestMatch[1]){
                bestMatch[0] = lit_pos;
                bestMatch[1] = best-1; //taking back the increment from the last loop cycle
            }
            lit_pos = checkFromPosition;
        }
        System.out.println(Arrays.toString(bestMatch));
        return bestMatch;
    }
    

    private static void createDataBlock(byte[] literals, int startOfLiterals, int startOfMatch, int matchLength) {

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

        System.out.println("mL: " + matchLength);
        System.out.println("pos: " + pos);

        // Processing the offset
        String offset = "";
        if(matchLength > pos){
            offset = String.format("%16s", Integer.toBinaryString(pos)).replace(' ',
            '0');
        } else {
            offset = String.format("%16s", Integer.toBinaryString(pos - matchLength)).replace(' ',
                '0');
        }
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

    public static void dataTraverse(byte[] data) {

        int matchLen = 0;
        byte[] potentialMatch = new byte[0];
        byte literalToCheck;

        // For the entirety of the data (minus the trailing 5 bytes)
        for (int i = 0; i < FILESIZE - 5; i++) {

            int startOfLiterals = pos;
            
            potentialMatch = Arrays.copyOf(potentialMatch, potentialMatch.length + 1);
            potentialMatch[potentialMatch.length - 1] = data[pos];
            literalToCheck = potentialMatch[potentialMatch.length - 1];
                        
            potentialMatch = Arrays.copyOfRange(data, 0, pos); // get data on LHS from current pos

            System.out.println("\npos: " + pos);
            System.out.println("literalsToCopy: " + Arrays.toString(potentialMatch));        
            System.out.println("literalToCheck (in potentialMatch): " + (int)literalToCheck + " (" + (char)literalToCheck + ")");

            ArrayList<int[]> matches = findFirstMatch(potentialMatch, literalToCheck);

            // If there are no matches, increase pos and continue
            if (matches.size() <= 0) {
                System.out.println("No matches");
                pos++;
                continue;
            } else {
                // best match from position of first match
                int[] bestMatch = findBestMatches(matches, pos);

                System.out.println("Best match (fromPos, matchLen): " + Arrays.toString(bestMatch));
                                
                int matchStart = bestMatch[0];
                matchLen = bestMatch[1];
                
                if(matchLen < 4){
                    System.out.println("MatchLen < 4");
                    pos++;
                    continue;
                }

                if (potentialMatch.length <= matchLen) {

                    if(potentialMatch.length <= 0){
                        pos = pos + matchLen + 1;
                    } else {
                        pos = pos + matchLen;
                    }

                    createDataBlock(potentialMatch, startOfLiterals, matchStart, matchLen);
                   
                    System.out.println("pos: " + pos + " , matchlen: " + matchLen + " , literals: " + lz4block.getSymbols().length);
                    
                    matchLen = 0;
                    potentialMatch = new byte[0];

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