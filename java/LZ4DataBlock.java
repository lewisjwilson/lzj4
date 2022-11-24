import java.util.Arrays;

public class LZ4DataBlock {
    private byte token;
    private byte[] symbols;
    private String offset;

    // Getters
    public byte getToken() {
        return token;
    }

    public byte[] getSymbols() {
        return symbols;
    }

    public String getOffset() {
        return offset;
    }

    public byte[] createDataBlock() {
        int blockLen = 1 + this.getSymbols().length + 2;
        byte[] dataBlock = new byte[blockLen];

        dataBlock[0] = Byte.valueOf(this.getToken());

        System.arraycopy(symbols, 0, dataBlock, 1, symbols.length);

        String offset1Hex = this.getOffset().substring(0, 2);
        int offset1Int = Integer.parseInt(offset1Hex, 16);
        String offset2Hex = this.getOffset().substring(2, 4);
        int offset2Int = Integer.parseInt(offset2Hex, 16);

        dataBlock[blockLen - 2] = (byte)offset1Int;
        dataBlock[blockLen - 1] = (byte)offset2Int;


        System.out.println("Data Block: " + Arrays.toString(dataBlock));

        return dataBlock;

    }

    // Setters
    public void setToken(byte token) {
        this.token = token;
    }

    public void setSymbols(byte[] symbols) {
        // Populating dataBlock with symbols (byte representation)
        this.symbols = symbols;
    }

    public void setOffset(String offsetByte1, String offsetByte2) {
        this.offset = offsetByte1 + offsetByte2;
    }

}
