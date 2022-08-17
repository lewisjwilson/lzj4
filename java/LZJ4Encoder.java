import java.util.ArrayList;

class LZJ4Encoder {

    // Data variables
    static String testData = "abbccabbcccabbaabcc";
    static int dataLen = testData.length();

    // Search buffer variables
    static int windowBuf = dataLen; // temporarily - to encode all data at once
    static int pos = 0;

    public void updateBuffer() {
        // TODO
    }

    public static String getWindow() {
        if (windowBuf > dataLen) {
            return testData.substring(pos, dataLen);
        } else {
            return testData.substring(pos, windowBuf);
        }
    }

    // Method to find the indexes of substring matches in a string
    public static ArrayList<Integer> findMatches(String str, String subStr) {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        for (int i = -1; (i = str.indexOf(subStr, i + 1)) != -1; i++) {
            if (i == 0) {
                continue; // remove the case where substring match is the beginning of the string
            }
            matches.add(i);
        }
        return matches;
    }

    public static void dataEncode(String window) {

        String bestMatch = "", subStr = "";

        for (int i = pos; i < windowBuf; i++) {
            String curByte = Character.toString(testData.charAt(i)); // Get the current byte from the window
            subStr = subStr.concat(curByte); // Add the current byte to the subStr ArrayList

            // Matches must be >= 4
            if (subStr.length() < 5) {
                continue;
            }
            System.out.println("SubStr: " + subStr);
            ArrayList<Integer> matches = findMatches(window, subStr);
            System.out.println("Matches: " + matches);

            // If there are no matches, reset the subStr variable and continue
            if (matches.isEmpty()) {
                subStr = "";
                continue;
            } else {
                // If the length of the current best match is less than the length of the
                // current substring
                if (bestMatch.length() < subStr.length()) {
                    // Replace the best match
                    bestMatch = subStr;
                }

            }
            System.out.println("Best Match: " + bestMatch);

        }
    }

    public static void main(String[] args) {

        String window = getWindow();
        System.out.println(window);
        dataEncode(window);

    }

}