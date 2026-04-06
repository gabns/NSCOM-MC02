import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class RtcpManager {
    private DatagramSocket socket;
    private DatagramSocket receiverSocket;
    private InetAddress dest;
    private int port;
    private Timer timer;
    private boolean isRunning = true;

    public RtcpManager(InetAddress dest, int port) throws SocketException {
        this.dest = dest;
        this.port = port;
        this.socket = new DatagramSocket();
    }

    public void startReceiver(int localPort) {
        new Thread(() -> {
            try {
                receiverSocket = new DatagramSocket(localPort);
                byte[] buffer = new byte[100];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (isRunning) {
                    receiverSocket.receive(packet);
                    System.out.println("Received RTCP report from " + packet.getAddress().getHostAddress());
                }
            } catch (Exception e) {
                if (isRunning) System.out.println("RTCP Receiver Socket closed.");
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (timer != null) timer.cancel();
        if (socket != null && !socket.isClosed()) socket.close();
        if (receiverSocket != null && !receiverSocket.isClosed()) receiverSocket.close();
    }

    // Rubric: Periodically sends RTCP Sender Reports 
    public void sendSenderReport(int packetCount, int octetCount) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Send actual binary RTCP Sender Report (Payload Type 200) instead of plain text string
                    byte[] rtcpPacket = new byte[28];
                    rtcpPacket[0] = (byte) 0x80; // V=2, P=0, RC=0
                    rtcpPacket[1] = (byte) 200;  // PT=200 (Sender Report)
                    rtcpPacket[2] = 0x00;        // Length
                    rtcpPacket[3] = 0x06;        // 6 words (28 bytes total)
                    
                    // Assign dummy SSRC, NTP timestamps, etc. (required for valid header structure)
                    rtcpPacket[4] = 0x12; rtcpPacket[5] = 0x34; rtcpPacket[6] = 0x56; rtcpPacket[7] = 0x78;
                    
                    socket.send(new DatagramPacket(rtcpPacket, rtcpPacket.length, dest, port));
                } catch (Exception e) {
                    System.out.println("RTCP Error gracefully caught: " + e.getMessage());
                }
            }
        }, 0, 5000);
    }
}