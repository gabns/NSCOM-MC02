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
// Real-time Microphone Feature (with on-the-fly conversion)
    public void startMicSender(InetAddress dest, int port) {
        new Thread(() -> {
            try (DatagramSocket rtpSocket = new DatagramSocket()) {
                
                // 1. Ask the hardware for a standard format it actually supports (44.1kHz, 16-bit, Mono)
                AudioFormat hardwareFormat = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, hardwareFormat);

                if (!AudioSystem.isLineSupported(info)) {
                    System.out.println("Microphone hardware format not supported!");
                    return;
                }

                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(hardwareFormat);
                mic.start();

                System.out.println("Microphone active. Converting live audio to 8000Hz 8-bit...");

                
                AudioInputStream rawMicStream = new AudioInputStream(mic);

                
                AudioFormat targetFormat = new AudioFormat(8000, 8, 1, true, false);

                
                AudioInputStream convertedMicStream = AudioSystem.getAudioInputStream(targetFormat, rawMicStream);

                byte[] buffer = new byte[160];
                int seqNum = 0;
                int timestamp = 0;
                int ssrc = 99999; 

                int bytesRead;
                
                // 5. Read from the converted stream instead of the raw mic
                while (isRunning && (bytesRead = convertedMicStream.read(buffer, 0, buffer.length)) != -1) {
                    if (bytesRead > 0) {
                        // Prevent buffer bleed
                        byte[] actualData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, actualData, 0, bytesRead);

                        RtpPacket rtp = new RtpPacket(seqNum, timestamp, ssrc, actualData);
                        byte[] packetData = rtp.toNetworkBytes();

                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dest, port);
                        rtpSocket.send(packet);

                        seqNum++;
                        timestamp += 160;
                        
                        // Note: No Thread.sleep() needed here! 
                        // The physical microphone paces the loop in real-time automatically.
                    }
                }

                // Cleanup resources
                convertedMicStream.close();
                rawMicStream.close();
                mic.drain();
                mic.close();
                System.out.println("Microphone stream finished.");

            } catch (Exception e) {
                if (isRunning) System.out.println("Mic Sender Error: " + e.getMessage());
            }
        }).start();
    }

// Natively Converting File Sender
    public void startSender(File audioFile, InetAddress dest, int port) {
        new Thread(() -> {
            try (DatagramSocket rtpSocket = new DatagramSocket()) {

                System.out.println("File RTP Sender Thread Started. Streaming to port: " + port);
                
                //get original audio format
                AudioInputStream originalStream = AudioSystem.getAudioInputStream(audioFile);
                
                //define target/acceptable format
                AudioFormat targetFormat = new AudioFormat(8000, 8, 1, true, false);
                
                //convert into streamable format
                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
                
                byte[] buffer = new byte[160];
                int seqNum = 0;
                int timestamp = 0;
                int ssrc = 12345;

                //read from converted stream and send
                long startTime = System.currentTimeMillis();
                long packetsSent = 0;
                int bytesRead;

                while (isRunning && (bytesRead = convertedStream.read(buffer, 0, buffer.length)) != -1) {
                    if (bytesRead > 0) {
                        //prevent buffer bleed
                        byte[] actualData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, actualData, 0, bytesRead);
        
                        RtpPacket rtp = new RtpPacket(seqNum, timestamp, ssrc, actualData);
                        byte[] packetData = rtp.toNetworkBytes();

                        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, dest, port);
                        rtpSocket.send(packet);

                        seqNum++;
                        timestamp += 160;
                        packetsSent++; //track num of sent packets

                        //drift compensation 
                        long targetTime = startTime + (packetsSent * 20);
                        long sleepTime = targetTime - System.currentTimeMillis();

                        //sleep only if ahead 
                        if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
        }
    }
}
                
                convertedStream.close();
                originalStream.close();
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