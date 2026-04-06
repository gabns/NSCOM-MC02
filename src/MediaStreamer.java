import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class MediaStreamer {

    private boolean isRunning = true;
    private DatagramSocket rtpReceiverSocket;

    public void stopStreaming() {
        isRunning = false;
        if (rtpReceiverSocket != null && !rtpReceiverSocket.isClosed()) {
            rtpReceiverSocket.close();
        }
    }

    // Real-time Microphone Feature
    public void startMicSender(InetAddress dest, int port) {
        new Thread(() -> {
            try (DatagramSocket rtpSocket = new DatagramSocket()) {
                
                // Standard 8000Hz, 8-bit, Mono PCM Signed format
                AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Microphone line not supported!");
                    return;
                }

                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(format);
                mic.start();

                System.out.println("Microphone RTP Sender Started. Streaming live audio to port: " + port);

                byte[] buffer = new byte[160];
                int seqNum = 0;
                int timestamp = 0;
                int ssrc = 99999; // Different SSRC for mic

                while (isRunning) {
                    int bytesRead = mic.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        RtpPacket rtp = new RtpPacket(seqNum, timestamp, ssrc, buffer);
                        byte[] packetData = rtp.toNetworkBytes();

                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dest, port);
                        rtpSocket.send(packet);

                        seqNum++;
                        timestamp += 160;
                    }
                }

                mic.drain();
                mic.close();
                System.out.println("Microphone stream finished.");

            } catch (Exception e) {
                if (isRunning) System.out.println("Mic Sender Error: " + e.getMessage());
            }
        }).start();
    }

    // Original File Sender
    public void startSender(File audioFile, InetAddress dest, int port) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(audioFile);
                 DatagramSocket rtpSocket = new DatagramSocket()) {

                System.out.println("File RTP Sender Thread Started. Streaming to port: " + port);
                
                // Skip the 44-byte WAV header to avoid loud static pop
                if (audioFile.getName().toLowerCase().endsWith(".wav")) {
                    fis.skip(44); 
                }
                
                byte[] buffer = new byte[160];
                int seqNum = 0;
                int timestamp = 0;
                int ssrc = 12345;

                while (isRunning && fis.read(buffer) != -1) {
                    RtpPacket rtp = new RtpPacket(seqNum, timestamp, ssrc, buffer);
                    byte[] packetData = rtp.toNetworkBytes();

                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dest, port);
                    rtpSocket.send(packet);

                    seqNum++;
                    timestamp += 160;

                    Thread.sleep(20); // 160 bytes at 8000Hz is exactly 20ms of audio
                }
                
                System.out.println("File stream finished.");

            } catch (Exception e) {
                if (isRunning) System.out.println("Sender Error: " + e.getMessage());
            }
        }).start();
    }

    public void startReceiver(int localPort) {
        new Thread(() -> {
            try {
                rtpReceiverSocket = new DatagramSocket(localPort);
                System.out.println("RTP Receiver Thread Started. Listening on port: " + localPort);

                // Use Standard 8000Hz PCM Signed to match the Microphone & stripped WAV
                AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Audio playback line not supported.");
                    return;
                }

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                // 12 bytes for header + 160 for payload
                byte[] buffer = new byte[172]; 
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (isRunning) {
                    try {
                        rtpReceiverSocket.receive(packet);
                        int payloadSize = packet.getLength() - 12;
                        line.write(packet.getData(), 12, payloadSize);
                    } catch (SocketException se) {
                        // Socket explicitly closed via stopStreaming()
                        break;
                    }
                }

                line.drain();
                line.close();
                if (!rtpReceiverSocket.isClosed()) {
                    rtpReceiverSocket.close();
                }

            } catch (Exception e) {
                if (isRunning) System.out.println("Receiver Error: " + e.getMessage());
            }
        }).start();
    }
}