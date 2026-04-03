import java.net.*;

public class SipClient {
    private DatagramSocket socket;
    
    // Rubric: Includes all required SIP headers 
    public void sendInvite(String destinationIp, int port, String sdpData) {
        // TODO: Construct the SIP INVITE string [cite: 23, 44]
        // TODO: Ensure SDP is correctly embedded in the body [cite: 45, 81]
    }

    public void processResponse() {
        // TODO: Receive packet and parse for "200 OK" [cite: 31, 81]
        // TODO: Parse remote IP/Port/Codec from the SDP body 
    }

    public void sendAck() {
        // TODO: Send the ACK to confirm call establishment [cite: 34, 44]
    }

    // Rubric: Uses BYE message to gracefully end the call [cite: 37, 46, 81]
    public void sendBye() {
        // TODO: Send BYE and close sockets/free resources 
    }
}