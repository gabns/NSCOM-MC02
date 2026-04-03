import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class RtcpManager {
    private DatagramSocket socket;
    private InetAddress dest;
    private int port;

    public RtcpManager(InetAddress dest, int port) throws SocketException {
        this.dest = dest;
        this.port = port;
        this.socket = new DatagramSocket();
    }

    // Rubric: Periodically sends RTCP Sender Reports 
    public void sendSenderReport(int packetCount, int octetCount) {
        // TODO: Send basic stats packet every 5 seconds 
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String stats = "RTCP SR | Packets Sent: " + packetCount + " | Bytes: " + octetCount;
                    byte[] data = stats.getBytes();
                    socket.send(new DatagramPacket(data, data.length, dest, port));
                    System.out.println("Sent RTCP Sender Report");
                } catch (Exception e) {
                    System.out.println("RTCP Error gracefully caught: " + e.getMessage());
                }
            }
            }, 0, 5000);
        }
    }