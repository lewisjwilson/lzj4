import java.util.ArrayList;
import java.util.List;

class LZJ4Encoder {

    // Data variables
    static String testData = "abbccabbcccabbaabcc";
    static int dataLen = testData.length();

    // Window buffer and search buffer variables
    static int windowBuf = 100000; // temporarily - to encode all data at once
    static int searchBuf = 100000; // temporarily - to encode all data at once

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

    public static List<Byte> createDataBlock(String symbols, int cursorPosition, int matchLength,
            ArrayList<Integer> matches) {
        String hiToken = String.format("%04d", Integer.toBinaryString(matchLength));
        String loToken = String.format("%04d", Integer.toBinaryString(matchLength - 1));
        String token = hiToken + loToken;

        // offset is the current buffer length minus the index of a match
        int offset = buffer.length() - matches.get(0); // First element in matches

        System.out.println("pos: " + cursorPosition);
        System.out.println("symbols: " + symbols);
        System.out.println("hi: " + hiToken);
        System.out.println("lo: " + loToken);
        System.out.println("token: " + token);
        System.out.println("offset: " + offset);

        List<Byte> dataBlock = new ArrayList<Byte>();
        // dataBlock.add();

        // incrementing pos
        pos = pos + matchLength;
        return dataBlock;
    }

    public static void dataTraverse(String window) {

        while (pos < dataLen) {
            // System.out.println("pos: " + pos);

            String bestMatch = "";
            String subStr = "";
            int matchLen = 0;

            for (int c = 0; c < window.length(); c++) {
                // System.out.println("pos: " + pos);

                // Matches must be >= 4
                if (c < 5) {
                    pos++;
                    continue;
                }

                window = getWindow();
                buffer = getBuffer();
                System.out.println("window: " + window);
                System.out.println("buffer: " + buffer);

                subStr = window.substring(0, c); // get the current subscrting from the window
                System.out.println("subStr: " + subStr);

                ArrayList<Integer> matches = findMatches(buffer, subStr);
                System.out.println("Matches at pos: " + matches);

                // If there are no matches, reset the subStr variable and continue
                if (matches.isEmpty()) {
                    subStr = "";
                    pos++;
                    continue;
                } else {

                    // If the length of the current best match is less than the length of the
                    // current substring
                    matchLen = subStr.length();
                    if (bestMatch.length() < matchLen) {
                        // Replace the best match
                        bestMatch = subStr;
                        createDataBlock(bestMatch, pos, matchLen, matches);
                    }
                    subStr = "";
                }

            }

            // System.out.println("Best Match: " + bestMatch);
            // System.out.println("Match length: " + matchLen);

        }

    }

    public static void main(String[] args) {

        dataTraverse(window);

    }

}