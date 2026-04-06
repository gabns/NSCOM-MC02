public class RtpPacket {
    // Rubric: Builds valid RTP headers (sequence, timestamp, SSRC) 
    private byte[] header = new byte[12];
    private byte[] payload;

    public RtpPacket(int seqNum, int timestamp, int ssrc, byte[] data) {
        header[0] = (byte) 0x80; // Version 2, padding 0, extension 0, CSRC count 0
        header[1] = (byte) 0x00; // Marker 0, Payload Type 0 (PCMU)

        // Sequence Number
        header[2] = (byte) (seqNum >> 8);
        header[3] = (byte) (seqNum & 0xFF);

        // Timestamp
        header[4] = (byte) (timestamp >> 24);
        header[5] = (byte) ((timestamp >> 16) & 0xFF);
        header[6] = (byte) ((timestamp >> 8) & 0xFF);
        header[7] = (byte) (timestamp & 0xFF);

        // SSRC
        header[8] = (byte) (ssrc >> 24);
        header[9] = (byte) ((ssrc >> 16) & 0xFF);
        header[10] = (byte) ((ssrc >> 8) & 0xFF);
        header[11] = (byte) (ssrc & 0xFF);
        
        this.payload = data;
    }

    public byte[] toNetworkBytes() {
        byte[] packet = new byte[header.length + payload.length];
        System.arraycopy(header, 0, packet, 0, header.length);
        System.arraycopy(payload, 0, packet, header.length, payload.length);
        return packet;
    }
}