import java.util.Arrays;

public class LZ4DataBlock {
    private byte token;
    private byte[] hiTokenPlus;
    private byte[] loTokenPlus;
    private byte[] symbols;
    private String offset;

    // Getters
    public byte getToken() {
        return token;
    }

    public byte[] getHiTokenPlus() {
        // if this field is not used, return an empty array of size 0
        return (hiTokenPlus == null) ? new byte[0] : hiTokenPlus;
    }

    public byte[] getLoTokenPlus() {
        // if this field is not used, return an empty array of size 0
        return (loTokenPlus == null) ? new byte[0] : loTokenPlus;
    }

    public byte[] getSymbols() {
        return symbols;
    }
    
    public String getOffset() {
        return offset;
    }

    
    // Setters
    public void setToken(byte token) {
        this.token = token;
    }

    public void setHiTokenPlus(byte[] hiTokenPlus) {
        this.hiTokenPlus = hiTokenPlus;
    }

    public void setLoTokenPlus(byte[] loTokenPlus) {
        this.loTokenPlus = loTokenPlus;
    }

    public void setSymbols(byte[] symbols) {
        // Populating dataBlock with symbols (byte representation)
        this.symbols = symbols;
    }

    public void setOffset(String offsetByte1, String offsetByte2) {
        this.offset = offsetByte1 + offsetByte2;
    }


    public byte[] createDataBlock() {
        int blockLen = 1                                // token
                        + this.getHiTokenPlus().length  // literals+
                        + this.getSymbols().length      // literals
                        + 2                             // offset
                        + this.getLoTokenPlus().length; // matchLen+
        byte[] dataBlock = new byte[blockLen];
        
        int position = 0;

        // Token
        dataBlock[position] = Byte.valueOf(this.getToken());
        
        position++;
        
        // Literals+
        if(hiTokenPlus != null){
            System.arraycopy(hiTokenPlus, 0, dataBlock, position, hiTokenPlus.length);
            position += hiTokenPlus.length;
        }
        
        // Literals
        System.arraycopy(symbols, 0, dataBlock, position, symbols.length);
        
        position += symbols.length;
        
        // Offset
        String offset1Hex = this.getOffset().substring(0, 2);
        int offset1Int = Integer.parseInt(offset1Hex, 16);
        String offset2Hex = this.getOffset().substring(2, 4);
        int offset2Int = Integer.parseInt(offset2Hex, 16);
        dataBlock[position] = (byte)offset1Int;
        dataBlock[position+1] = (byte)offset2Int;
        
        position += 2;
        
        // Match Length+
        if(loTokenPlus != null){
            System.arraycopy(loTokenPlus, 0, dataBlock, position, loTokenPlus.length);
        }
        
        System.out.println("Data Block: " + Arrays.toString(dataBlock));

        return dataBlock;

    }

}
