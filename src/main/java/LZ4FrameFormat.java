public class LZ4FrameFormat {

    // Defines the format of the LZ4 data

    // Magic Number (4 Bytes)
    protected static byte[] magicNumber(){
        return new byte[]{0x04, 0x22, 0x4d, 0x18};
    }

    protected static byte[] frameDescriptor(){
        // Setting FLG byte (denoted as a binary string)

        // Bits 7-6: Version Number
        // From spec: "must be set to 01. Any other value cannot be decoded by this version of the specification."
        String flg = "01";

        // Bit 5: Block Independence Flag
        // From spec: "If this flag is set to “1”, blocks are independent. If this flag is set to “0”, 
        // each block depends on previous ones (up to LZ4 window size, which is 64 KB). In such case, it’s necessary to decode all blocks in sequence.""
        flg += "1";



    }


    
}
