# NSCOM-MC02
<h3>How to use</h3>
<p1>The program has an intuitive and simple way of sending files.</p1>
<ol>
    <li>Run the program via "java VoIPMain" on the command line of your choice.</li>
    <li>Select the type of client the program will be(caller/callee)</li>
    <li>Enter local SIP port to listen</li>
    <li>Enter Local RTP port to recieve audio</li>
    <li>Enter audio Source(mic,audio file.wav,none)</li>
    <li>Redo from second step if user wants to start another call or type exit to stop program</li>
</ol>
<h3>Implemented Features</h3>
    <p1>This project implements a fully functional, peer-to-peer Voice over IP (VoIP) application in Java. The architecture is modular, dividing the workload between session signaling, media encapsulation, concurrent streaming, and quality monitoring. </p1>
    <ol>
    <h4>SIP Signaling and Handshake (SipClient.java)</h4>
    <ul>
        <li>Three-Way Handshake: Strictly adheres to the SIP protocol to establish sessions using the INVITE -> 200 OK <- ACK sequence.</li>
        <li>Dynamic SDP Negotiation: Embeds Session Description Protocol (SDP) bodies within the INVITE and intelligently parses the 200 OK response to dynamically extract the remote client's RTP port.</li>
        <li>Graceful Teardown: Implements the BYE message sequence to cleanly terminate active calls and release network socket resources.</li>
        <li>Local Network Optimization: Operates over UDP using fixed loopback addressing (127.0.0.1) for reliable local testing without NAT or proxy overhead.</li>
    </ul>
    <h4>Media Encapsulation (RtpPacket.java)</h4>
    <ul>
        <li>Manual Packetization: Constructs valid 12-byte RTP headers from scratch via bit-shifting.</li>
        <li>Protocol Compliance: Accurately maintains and increments sequence numbers, synchronizes timestamps, and assigns unique Synchronization Source (SSRC) identifiers for both file and live-microphone streams.</li>
    </ul>
    <h4>Concurrent Media Streaming (MediaStreamer.java)</h4>
    <ul>
        <li>Dynamic File Conversion: Utilizes Java's AudioSystem to automatically intercept and downsample standard .wav files into the required low-bandwidth format (8000Hz, 8-bit, Mono, Signed PCM) on the fly, eliminating the need for manual file pre-processing.</li>
        <li>Real-Time Drift Compensation: Replaces standard thread sleeping with a real-time drift calculation algorithm. This ensures packets are dispatched in a strictly "timely manner" (exactly 20ms intervals), preventing receiver buffer starvation and audio choppiness.</li>
        <li>Buffer Bleed Prevention: Dynamically sizes byte arrays during file and microphone reads to prevent robotic audio echoing at the end of streams.</li>
    </ul>
    <h4>[BONUS] Two-Way Microphone Communication</h4>
    <ul>
    <li>Live Audio Capture: Bypasses static file streaming to capture real-time audio directly from the host machine's microphone hardware.</li>
    <li>On-The-Fly Downsampling: Captures audio at hardware-supported high-definition rates (44.1kHz, 16-bit) and instantly converts it to the 8kHz, 8-bit format required for lightweight UDP transmission.</li>
    <li>Full-Duplex Capability: The multi-threaded design allows both the Caller and Callee to seamlessly send and receive live audio concurrently.</li>
    </ul>
    <h4>RTCP Quality Monitoring (RtcpManager.java)</h4>
    <ul>
    <li>Standardized Sender Reports: Periodically dispatches binary RTCP Sender Reports (Payload Type 200) every 5 seconds.</li>
    <li>Port Synchronization: Automatically binds RTCP traffic to the designated RTP port + 1, ensuring control data and media data remain untangled.</li>
    </ul>
    </ol>
<h3>Test Cases</h3>