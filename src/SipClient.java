import java.net.*;

public class SipClient {
    private DatagramSocket socket;
    private InetAddress remoInetAddress;
    private int remotePort;
    
    private final String FIXED_LOCAL_IP = "127.0.0.1";
    
    public SipClient(int localPort) throws SocketException {
        this.socket = new DatagramSocket(localPort);
    }

    public InetAddress getRemoteAddress() {
        return this.remoInetAddress;
    }

    // Callee side SIP handshake listener
    public int listenForHandshake(int localRtpPort) throws Exception {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        System.out.println("Waiting for incoming SIP INVITE...");
        
        socket.receive(packet);
        String request = new String(packet.getData(), 0, packet.getLength());
        
        if (request.startsWith("INVITE")) {
            this.remoInetAddress = packet.getAddress();
            this.remotePort = packet.getPort();
            System.out.println("Received INVITE from " + this.remoInetAddress.getHostAddress() + ":" + this.remotePort);

            // Parse remote RTP port from Caller's SDP
            int remoteRtpPort = -1;
            String targetPortString = "m=audio ";
            int idx = request.indexOf(targetPortString);
            if (idx != -1) {
                int startIdx = idx + targetPortString.length();
                int endIdx = request.indexOf(" ", startIdx);
                remoteRtpPort = Integer.parseInt(request.substring(startIdx, endIdx).trim());
                System.out.println("Parsed remote RTP port: " + remoteRtpPort);
            }

            // Construct 200 OK with Callee's SDP
            String sdp = "v=0\r\n" +
                         "o=- 0 0 IN IP4 " + FIXED_LOCAL_IP + "\r\n" +
                         "s=Sip Call\r\n" +
                         "c=IN IP4 " + FIXED_LOCAL_IP + "\r\n" +
                         "t=0 0\r\n" +
                         "m=audio " + localRtpPort + " RTP/AVP 0\r\n" +
                         "a=rtpmap:0 PCMU/8000\r\n";

            String okResponse = "SIP/2.0 200 OK\r\n" +
                                "Via: SIP/2.0/UDP " + this.remoInetAddress.getHostAddress() + ":" + this.remotePort + "\r\n" +
                                "From: <sip:caller@" + this.remoInetAddress.getHostAddress() + ">;tag=12345\r\n" +
                                "To: <sip:callee@" + FIXED_LOCAL_IP + ">;tag=67890\r\n" +
                                "Call-ID: 1234567890@client\r\n" +
                                "CSeq: 1 INVITE\r\n" +
                                "Content-Type: application/sdp\r\n" +
                                "Content-Length: " + sdp.length() + "\r\n\r\n" +
                                sdp;

            byte[] msg = okResponse.getBytes();
            socket.send(new DatagramPacket(msg, msg.length, this.remoInetAddress, this.remotePort));
            System.out.println("Sent 200 OK");

            // Wait for ACK
            socket.receive(packet);
            String ackRequest = new String(packet.getData(), 0, packet.getLength());
            if (ackRequest.startsWith("ACK")) {
                System.out.println("Received ACK. Handshake complete.");
                return remoteRtpPort;
            }
        }
        return -1;
    }

    // Callee listens for BYE message
    public void listenForBye() throws Exception {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        System.out.println("Waiting for BYE...");
        socket.receive(packet);
        String request = new String(packet.getData(), 0, packet.getLength());
        
        if (request.startsWith("BYE")) {
            System.out.println("Received BYE. Sending 200 OK...");
            String okResponse = "SIP/2.0 200 OK\r\n" +
                                "Via: SIP/2.0/UDP " + this.remoInetAddress.getHostAddress() + ":" + this.remotePort + "\r\n" +
                                "From: <sip:caller@" + this.remoInetAddress.getHostAddress() + ">;tag=12345\r\n" +
                                "To: <sip:callee@" + FIXED_LOCAL_IP + ">;tag=67890\r\n" +
                                "Call-ID: 1234567890@client\r\n" +
                                "CSeq: 2 BYE\r\n\r\n";
            byte[] msg = okResponse.getBytes();
            socket.send(new DatagramPacket(msg, msg.length, this.remoInetAddress, this.remotePort));
            socket.close();
        }
    }

    // Caller initiates the INVITE
    public void sendInvite(String destinationIp, int remoteSipPort, int localRtpPort) throws Exception {
        this.remoInetAddress = InetAddress.getByName(destinationIp);
        this.remotePort = remoteSipPort;
        
        String sdp = "v=0\r\n" +
                     "o=- 0 0 IN IP4 " + FIXED_LOCAL_IP + "\r\n" +
                     "s=Sip Call\r\n" +
                     "c=IN IP4 " + FIXED_LOCAL_IP + "\r\n" +
                     "t=0 0\r\n" +
                     "m=audio " + localRtpPort + " RTP/AVP 0\r\n" +
                     "a=rtpmap:0 PCMU/8000\r\n";

        String invite = "INVITE sip:" + destinationIp + ":" + remoteSipPort + " SIP/2.0\r\n" +
                        "Via: SIP/2.0/UDP " + FIXED_LOCAL_IP + ":" + socket.getLocalPort() + "\r\n" +
                        "From: <sip:client@" + FIXED_LOCAL_IP + ">;tag=12345\r\n" +
                        "To: <sip:" + destinationIp + ":" + remoteSipPort + ">\r\n" +
                        "Call-ID: 1234567890@client\r\n" +
                        "CSeq: 1 INVITE\r\n" +
                        "Content-Type: application/sdp\r\n" +
                        "Content-Length: " + sdp.length() + "\r\n\r\n" +
                        sdp;

        byte[] msg = invite.getBytes();
        socket.send(new DatagramPacket(msg, msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP INVITE to " + destinationIp + ":" + remoteSipPort);
    }

    public int processResponse() throws Exception {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        System.out.println("Waiting for SIP response...");
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength());

        if (response.contains("200 OK")) {
            System.out.println("Received 200 OK from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
            String targetPortString = "m=audio ";
            int idx = response.indexOf(targetPortString);
            if (idx != -1) {
                int startIdx = idx + targetPortString.length();
                int endIdx = response.indexOf(" ", startIdx);

                String portStr = response.substring(startIdx, endIdx).trim();
                System.out.println("Parsed remote RTP port: " + portStr);
                return Integer.parseInt(portStr);
            } 
        } else if(response.contains("SIP/2.0 4") || response.contains("SIP/2.0 5")) {
            System.out.println("Received unexpected response: " + response);
        }
        return -1; 
    }

    public void sendAck() throws Exception {
        String ack = "ACK sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + " SIP/2.0\r\n" +
                     "Via: SIP/2.0/UDP " + FIXED_LOCAL_IP + ":" + socket.getLocalPort() + "\r\n" +
                     "From: <sip:client@" + FIXED_LOCAL_IP + ">;tag=12345\r\n" +
                     "To: <sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + ">\r\n" +
                     "Call-ID: 1234567890@client\r\n" +
                     "CSeq: 1 ACK\r\n\r\n";

        byte[] msg = ack.getBytes();
        socket.send(new DatagramPacket(msg, msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP ACK to " + remoInetAddress.getHostAddress() + ":" + remotePort);
    }

    public void sendBye() throws Exception {
        String bye = "BYE sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + " SIP/2.0\r\n" +
                     "Via: SIP/2.0/UDP " + FIXED_LOCAL_IP + ":" + socket.getLocalPort() + "\r\n" +
                     "From: <sip:client@" + FIXED_LOCAL_IP + ">;tag=12345\r\n" +
                     "To: <sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + ">\r\n" +
                     "Call-ID: 1234567890@client\r\n" +
                     "CSeq: 2 BYE\r\n\r\n";
                     
        byte[] msg = bye.getBytes();
        socket.send(new DatagramPacket(msg, msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP BYE to " + remoInetAddress.getHostAddress() + ":" + remotePort);
        socket.close();
    }
}