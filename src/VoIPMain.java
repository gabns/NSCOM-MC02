import java.io.File;
import java.net.InetAddress;
import java.util.Scanner;

public class VoIPMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        boolean running = true;
        while (running) {
            System.out.print("Are you the Caller or Callee? (Enter 'caller', 'callee', or 'exit' to quit): ");
            String mode = scanner.nextLine().trim().toLowerCase();

            if (mode.equals("exit") || mode.equals("quit")) {
                System.out.println("Exiting program...");
                running = false;
                continue;
            }

            try {
                if (mode.equals("caller")) {
                    String remoteIp = "127.0.0.1"; 
                    System.out.println("Using fixed Remote SIP IP: " + remoteIp);
                    
                    System.out.print("Enter Remote SIP Port (e.g., 5060): ");
                    int remoteSipPort = Integer.parseInt(scanner.nextLine().trim());
                    
                    System.out.print("Enter Local RTP Port to receive audio (e.g., 9002): ");
                    int localRtpPort = Integer.parseInt(scanner.nextLine().trim());
                
                    System.out.print("Enter Audio Source ('mic' for microphone, or filename like 'audio.wav'): ");
                    String audioSource = scanner.nextLine().trim();

                    System.out.println("\nInitiating Call...");
                    
                    SipClient sipClient = new SipClient(0); 
                    sipClient.sendInvite(remoteIp, remoteSipPort, localRtpPort);
                    int remoteRtpPort = sipClient.processResponse();

                    if (remoteRtpPort != -1) {
                        sipClient.sendAck();
                        System.out.println(">>> Call established! <<<");

                        MediaStreamer streamer = new MediaStreamer();
                        streamer.startReceiver(localRtpPort);
                        
                        if (audioSource.equalsIgnoreCase("mic")) {
                            streamer.startMicSender(InetAddress.getByName(remoteIp), remoteRtpPort);
                        } else {
                            streamer.startSender(new File(audioSource), InetAddress.getByName(remoteIp), remoteRtpPort);
                        }

                        RtcpManager rtcpManager = new RtcpManager(InetAddress.getByName(remoteIp), remoteRtpPort + 1);
                        rtcpManager.startReceiver(localRtpPort + 1);
                        rtcpManager.sendSenderReport(100, 16000); // Trigger periodic reports

                        System.out.println("\n*** Press ENTER to end call... ***\n");
                        System.in.read();

                        System.out.println("Ending call...");
                        streamer.stopStreaming();
                        rtcpManager.stop();
                        sipClient.sendBye();
                    }

                } else if (mode.equals("callee")) {
                    System.out.print("Enter Local SIP Port to listen on (e.g., 5060): ");
                    int localSipPort = Integer.parseInt(scanner.nextLine().trim());
                    
                    System.out.print("Enter Local RTP Port to receive audio (e.g., 9000): ");
                    int localRtpPort = Integer.parseInt(scanner.nextLine().trim());
                    
                    System.out.print("Enter Audio Source for two-way communication ('mic', file name, or 'none'): ");
                    String audioSource = scanner.nextLine().trim();

                    SipClient sipClient = new SipClient(localSipPort);
                    System.out.println("\nCallee listening on SIP port " + localSipPort + "...");
                    
                    int remoteRtpPort = sipClient.listenForHandshake(localRtpPort);
                    
                    if (remoteRtpPort != -1) {
                        System.out.println(">>> Call established! Ready to receive audio. <<<");
                        
                        MediaStreamer streamer = new MediaStreamer();
                        streamer.startReceiver(localRtpPort);
                        
                        if (!audioSource.equalsIgnoreCase("none") && !audioSource.isEmpty()) {
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
                    System.out.println("Unknown mode selected. Please type 'caller', 'callee', or 'exit'.");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[ERROR] Invalid input. Please ensure you enter numerical values for ports.");
            } catch (java.net.BindException e) {
                System.out.println("\n[ERROR] Port already in use. Please select a different local port.");
            } catch (java.net.UnknownHostException e) {
                System.out.println("\n[ERROR] Unknown IP address. Please check the remote IP and try again.");
            } catch (Exception e) {
                System.out.println("\n[ERROR] An unexpected error occurred: " + e.getMessage());
            } 
            
            if (running) {
                System.out.println("\n--- Returning to menu ---\n");
            }
        }
        scanner.close();
    }
}