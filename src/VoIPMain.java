import java.io.File;
import java.net.InetAddress;

public class VoIPMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java VoIPMain <caller|callee> [options]");
            return;
        }

        String mode = args[0].toLowerCase();
        try {
            if (mode.equals("caller")) {
                if (args.length < 5) {
                    System.out.println("Usage: java VoIPMain caller <remoteSipIp> <remoteSipPort> <localRtpPort> <audioFile.wav | mic>");
                    return;
                }
                String remoteIp = args[1];
                int remoteSipPort = Integer.parseInt(args[2]);
                int localRtpPort = Integer.parseInt(args[3]);
                String audioSource = args[4];

                SipClient sipClient = new SipClient(0); 
                sipClient.sendInvite(remoteIp, remoteSipPort, localRtpPort);
                int remoteRtpPort = sipClient.processResponse();
                
                if (remoteRtpPort != -1) {
                    sipClient.sendAck();
                    System.out.println("Call established!");

                    MediaStreamer streamer = new MediaStreamer();
                    streamer.startReceiver(localRtpPort);
                    
                    // BONUS: Check if user wants microphone or file
                    if (audioSource.equalsIgnoreCase("mic")) {
                        streamer.startMicSender(InetAddress.getByName(remoteIp), remoteRtpPort);
                    } else {
                        streamer.startSender(new File(audioSource), InetAddress.getByName(remoteIp), remoteRtpPort);
                    }

                    RtcpManager rtcpManager = new RtcpManager(InetAddress.getByName(remoteIp), remoteRtpPort + 1);
                    rtcpManager.startReceiver(localRtpPort + 1);
                    rtcpManager.sendSenderReport(100, 16000); // Trigger periodic reports

                    System.out.println("Press ENTER to end call...");
                    System.in.read();

                    streamer.stopStreaming();
                    rtcpManager.stop();
                    sipClient.sendBye();
                }

            } else if (mode.equals("callee")) {
                if (args.length < 3) {
                    System.out.println("Usage: java VoIPMain callee <localSipPort> <localRtpPort> [audioFileToSendBack.wav | mic]");
                    return;
                }
                int localSipPort = Integer.parseInt(args[1]);
                int localRtpPort = Integer.parseInt(args[2]);

                SipClient sipClient = new SipClient(localSipPort);
                System.out.println("Callee listening on SIP port " + localSipPort);
                
                int remoteRtpPort = sipClient.listenForHandshake(localRtpPort);
                
                if (remoteRtpPort != -1) {
                    System.out.println("Call established! Ready to receive audio.");
                    
                    MediaStreamer streamer = new MediaStreamer();
                    streamer.startReceiver(localRtpPort);
                    
                    // BONUS: Optional two-way audio if callee passes a 4th argument (file or mic)
                    if (args.length >= 4) {
                        String audioSource = args[3];
                        if (audioSource.equalsIgnoreCase("mic")) {
                            streamer.startMicSender(sipClient.getRemoteAddress(), remoteRtpPort);
                        } else {
                            streamer.startSender(new File(audioSource), sipClient.getRemoteAddress(), remoteRtpPort);
                        }
                    }
                    
                    RtcpManager rtcpManager = new RtcpManager(sipClient.getRemoteAddress(), remoteRtpPort + 1);
                    rtcpManager.startReceiver(localRtpPort + 1);
                    rtcpManager.sendSenderReport(50, 8000); // Send its own reports

                    sipClient.listenForBye();
                    streamer.stopStreaming();
                    rtcpManager.stop();
                    System.out.println("Call ended.");
                }
            } else {
                System.out.println("Unknown mode: " + mode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}