import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class MediaStreamer {

    private boolean isRunning = true;

    public void stopStreaming() {
        isRunning = false;
    }

    public void startSender(File audioFile, InetAddress dest, int port) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(audioFile);
                 DatagramSocket rtpSocket = new DatagramSocket()) {

                System.out.println("RTP Sender Thread Started. Streaming to port: " + port);
                
                byte[] buffer = new byte[160];
                int seqNum = 0;
                int timestamp = 0;
                int ssrc = 12345;

                while (isRunning && fis.read(buffer) != -1) {
                    // Create the packet
                    RtpPacket rtp = new RtpPacket(seqNum, timestamp, ssrc, buffer);
                    byte[] packetData = rtp.toNetworkBytes();

                    // Send the packet
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dest, port);
                    rtpSocket.send(packet);

                    seqNum++;
                    timestamp += 160;

                    Thread.sleep(20);
                }
                
                System.out.println("File stream finished.");

            } catch (Exception e) {
                System.out.println("Sender Error: " + e.getMessage());
            }
        }).start();
    }

    public void startReceiver(int localPort) {
        new Thread(() -> {
            try (DatagramSocket rtpSocket = new DatagramSocket(localPort)) {
                
                System.out.println("RTP Receiver Thread Started. Listening on port: " + localPort);

                AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Audio line not supported.");
                    return;
                }

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                // 12 bytes for header + 160 for payload
                byte[] buffer = new byte[172]; 
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (isRunning) {
                    rtpSocket.receive(packet);

                    int payloadSize = packet.getLength() - 12;
                    line.write(packet.getData(), 12, payloadSize);
                }

                line.drain();
                line.close();

            } catch (Exception e) {
                System.out.println("Receiver Error: " + e.getMessage());
            }
        }).start();
    }
}