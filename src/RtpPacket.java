public class RtpPacket {
    // Rubric: Builds valid RTP headers (sequence, timestamp, SSRC) 
    private byte[] header = new byte[12];
    private byte[] payload;

    public RtpPacket(int seqNum, int timestamp, int ssrc, byte[] data) {
        // TODO: Bit-shift seqNum and timestamp into the 12-byte header 
        this.payload = data;
    }

    public byte[] toNetworkBytes() {
        // TODO: Return combined header + payload 
        return null;
    }
}