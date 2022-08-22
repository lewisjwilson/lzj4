import java.util.ArrayList;
import java.util.List;

class LZJ4Encoder {

    // Data variables
    static String testData = "abbccabbcccabbaabcc";
    static int dataLen = testData.length();
    static List<Byte> encodedData = new ArrayList<Byte>();

    // Window buffer and search buffer variables
    static int windowBuf = 100000; // temporarily - to encode all data at once
    static int searchBuf = 100000; // temporarily - to encode all data at once

    // Initialize the number of new symbols used for token value in lz4 data block
    public static int newSymbolsCount = -1;

    // Cursor position initialization
    public static int pos = 0;

    // Getting the data in the window (search forward)
    public static String window = getWindow();

    // Getting the data in the search buffer (search backward)
    public static String buffer = getBuffer();

    // Method to process the data in the buffer (dealing with end cases)
    public static String getBuffer() {
        if (pos - searchBuf < 0) {
            return testData.substring(0, pos);
        } else {
            return testData.substring(pos - searchBuf, pos);
        }
    }

    // Method to process the data in the window (dealing with end cases)
    public static String getWindow() {
        if (pos + windowBuf > dataLen) {
            return testData.substring(pos, dataLen);
        } else {
            return testData.substring(pos, pos + windowBuf);
        }
    }

    // Method to find the indexes of substring matches in a string
    public static ArrayList<Integer> findMatches(String str, String subStr) {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        for (int i = -1; (i = str.indexOf(subStr, i + 1)) != -1; i++) {
            matches.add(i);
        }
        return matches;
    }

    private static void createDataBlock(int matchLength, String symbols, ArrayList<Integer> matches,
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
        byte[] symbolsArr = symbols.substring(0, newSymbolsCount).getBytes();

        // Processing the offset
        String offset = String.format("%16s", Integer.toBinaryString(buffer.length() - matches.get(0))).replace(' ',
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
        for (int i = 0; i < dataBlock.length; i++) {
            encodedData.add(dataBlock[i]);
        }

    }

    public static void dataTraverse(String window) {

        while (pos < dataLen) {

            String bestMatch = "";
            int matchLen = 0;
            String subStr = "";
            buffer = getBuffer();

            for (int c = 0; c < window.length(); c++) {
                System.out.println("pos: " + pos);
                newSymbolsCount++;

                // Matches must be >= 4
                if (c < 4) {
                    pos++;
                    continue;
                }

                window = getWindow();

                if (window.length() <= 0) {
                    System.out.println("End of window reached");
                    break;
                }

                buffer = getBuffer();
                System.out.println("buffer: " + buffer);
                System.out.println("window: " + window);

                subStr = window.substring(0, c); // get the current substring from the window
                System.out.println("subStr: " + subStr);

                if (c == window.length()) {
                    byte[] remainingBytes = subStr.getBytes();
                    for (int i = 0; i < remainingBytes.length; i++) {
                        encodedData.add(remainingBytes[i]);
                    }

                }

                ArrayList<Integer> matches = findMatches(buffer, subStr);

                // If there are no matches, reset the subStr variable and continue
                if (matches.isEmpty()) {
                    subStr = "";
                    pos++;
                    continue;
                } else {
                    System.out.println("Match found at index " + matches.get(0));
                    // If the length of the current best match is less than the length of the
                    // current substring
                    matchLen = subStr.length();
                    if (bestMatch.length() < matchLen) {
                        // Replace the best match
                        bestMatch = subStr;
                        createDataBlock(matchLen, bestMatch, matches, newSymbolsCount);
                        // Symbols since the previous match
                        // As matchLen number of symbols is appended after the literals in the token
                        // we minus matchLen number of new Symbols
                        newSymbolsCount = -matchLen;
                        break;
                    }
                }

            }

        }
        encodedData.add(encodedData.size(), (byte) 0);
        encodedData.add(encodedData.size(), (byte) 0);
        encodedData.add(encodedData.size(), (byte) 0);
        encodedData.add(encodedData.size(), (byte) 0);
        System.out.println("End of data reached!");

    }

    public static void main(String[] args) {

        dataTraverse(window);
        System.out.println("Encoded LZ4 Data:\n" + encodedData);

    }

}