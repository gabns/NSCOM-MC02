import java.io.*;
import java.net.*;

public class MediaStreamer {
    // Rubric: Sends audio frames from a file in a timely manner 
    public void startSender(File audioFile, InetAddress dest, int port) {
        // TODO: Read file (.wav or G.711) 
        // TODO: Packetize and send via UDP at 20ms intervals 
    }

    // Rubric: Accurately receives and plays audio 
    public void startReceiver(int localPort) {
        // TODO: Listen for RTP packets 
        // TODO: Strip RTP header and send payload to audio player 
    }
}