import java.net.*;

public class SipClient {
    private DatagramSocket socket;
    private InetAddress remoInetAddress;
    private int remotePort;
    
    
    public SipClient(int localPort) throws SocketException{
        this.socket = new DatagramSocket(localPort);
    }
    // Rubric: Includes all required SIP headers 
    public void sendInvite(String destinationIp, int port, String sdpData) throws Exception {
        this.remoInetAddress = InetAddress.getByName(destinationIp);
        this.remotePort = port;
        
        // TODO: Construct the SIP INVITE string [cite: 23, 44]
        String sdp = "v=0\r\n" +
                     "o=- 0 0 IN IP4 " + InetAddress.getLocalHost().getHostAddress() + "\r\n" +
                     "s=Sip Call\r\n" +
                     "c=IN IP4 " + InetAddress.getLocalHost().getHostAddress() + "\r\n" +
                     "t=0 0\r\n" +
                     "m=audio " + port + " RTP/AVP 0\r\n" +
                     "a=rtpmap:0 PCMU/8000\r\n";
        // TODO: Ensure SDP is correctly embedded in the body [cite: 45, 81]
        String invite = "INVITE sip:" + destinationIp + ":" + port + " SIP/2.0\r\n" +
                        "Via: SIP/2.0/UDP " + InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort() + "\r\n" +
                        "From: <sip:client@" + InetAddress.getLocalHost().getHostAddress() + ">;tag=12345\r\n" +
                        "To: <sip:" + destinationIp + ":" + port + ">\r\n" +
                        "Call-ID: 1234567890@client\r\n" +
                        "CSeq: 1 INVITE\r\n" +
                        "Content-Type: application/sdp\r\n" +
                        "Content-Length: " + sdp.length() + "\r\n\r\n" +
                        sdp;

        byte[] msg = invite.getBytes();
        socket.send(new DatagramPacket(msg, msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP INVITE to " + destinationIp + ":" + port);
    }

    public int processResponse() throws Exception {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        System.out.println("Waiting for SIP response...");
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength());

        if(response.contains("200 OK")) {
            System.out.println("Received 200 OK from " + packet.getAddress().getHostAddress() + ":" + packet.getPort());
            String targetPortString = "m=audio ";
            int idx = response.indexOf(targetPortString);
            if (idx != -1) {
                int startIdx = idx + targetPortString.length();
                int endIdx = response.indexOf(" ", startIdx);

                String portStr = response.substring(startIdx, endIdx);
                System.out.println("Parsed remote RTP port: " + portStr);
                return Integer.parseInt(portStr);
        } 
    }
    else if(response.contains("SIP/2.0 4") || response.contains("SIP/2.0 5")) {
            System.out.println("Received unexpected response: " + response);
        }
    return -1; // Indicate failure to parse port
}
    

    public void sendAck() throws Exception {
        String ack = "ACK sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + " SIP/2.0\r\n" +
                     "Via: SIP/2.0/UDP " + InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort() + "\r\n" +
                     "From: <sip:client@" + InetAddress.getLocalHost().getHostAddress() + ">;tag=12345\r\n" +
                     "To: <sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + ">\r\n" +
                     "Call-ID: 1234567890@client\r\n" +
                     "CSeq: 1 ACK\r\n\r\n";

        byte[] msg = ack.getBytes();
        socket.send(new DatagramPacket(msg,msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP ACK to " + remoInetAddress.getHostAddress() + ":" + remotePort);
    }

    // Rubric: Uses BYE message to gracefully end the call 
    public void sendBye() throws Exception {

        String bye = "BYE sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + " SIP/2.0\r\n" +
                     "Via: SIP/2.0/UDP " + InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort() + "\r\n" +
                     "From: <sip:client@" + InetAddress.getLocalHost().getHostAddress() + ">;tag=12345\r\n" +
                     "To: <sip:" + remoInetAddress.getHostAddress() + ":" + remotePort + ">\r\n" +
                     "Call-ID: 1234567890@client\r\n" +
                     "CSeq: 2 BYE\r\n\r\n";
        // TODO: Send BYE and close sockets/free resources 
        byte[] msg = bye.getBytes();
        socket.send(new DatagramPacket(msg, msg.length, remoInetAddress, remotePort));
        System.out.println("Sent SIP BYE to " + remoInetAddress.getHostAddress() + ":" + remotePort);
        socket.close();
    }
}