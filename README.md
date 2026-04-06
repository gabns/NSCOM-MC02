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
<p>Below is the comprehensive test suite used to verify the functionality, resilience, and protocol compliance of the application.</p>
<h3>Phase 1: Functional Application Testing</h3>
<h4>Test Case 1: Standard 1-Way Call Setup (File to None)</h4>
<p><strong>Objective:</strong> Verify that a standard call can be established and audio from a file is successfully streamed to a listening callee.</p>
<ul>
	<li><strong>Expected Output:</strong> Console logs show the SIP handshake (<code>INVITE</code>, <code>200 OK</code>, <code>ACK</code>). Audio from <code>music.wav</code> plays clearly from the Callee's speakers.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> SIP handshake completed successfully (<code>INVITE</code> sent/received, <code>200 OK</code> processed, <code>ACK</code> sent). The audio file was dynamically converted and played clearly on the receiver's end without distortion.
	</li>
</ul>
<h4>Test Case 2: BONUS - 2-Way Live Microphone Streaming</h4>
<p><strong>Objective:</strong> Verify that live microphone capture works and can be transmitted bidirectionally simultaneously.</p>
<ul>
	<li><strong>Expected Output:</strong> Both clients output "Microphone RTP Sender Started". Audio spoken into the mic on Client A is heard on Client B's speakers, and vice versa.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Successfully established a 2-way microphone stream. Both clients outputted the correct startup logs, and live audio was clearly captured and heard in real-time bidirectionally.
	</li>
</ul>
<h4>Test Case 3: Graceful Call Teardown</h4>
<p><strong>Objective:</strong> Verify that the call ends elegantly, frees sockets, and returns to the menu.</p>
<ul>
	<li><strong>Expected Output:</strong> Caller sends a <code>BYE</code> message. Callee receives it and prints "Call ended." Both terminals loop back to the initial "Are you the Caller or Callee?" prompt without crashing.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Pressing ENTER instantly triggered the <code>BYE</code> sequence. Callee printed "Call ended", sockets were cleanly closed, and both terminals successfully looped back to the main menu without throwing any exceptions.</li>
</ul>
<h4>Test Case 4: Back-to-Back Call Execution</h4>
<p><strong>Objective:</strong> Verify that sockets are freed properly, allowing a second call to be made immediately without restarting the application.</p>
<ul>
	<li><strong>Expected Output:</strong> The second call connects and streams audio successfully, proving no <code>BindException</code> lingering from the previous call.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Initiated a second call immediately after the previous teardown using the exact same local and remote ports. Connected seamlessly without throwing a <code>BindException</code>, proving thorough socket teardown.</li>
</ul>
<h3>Phase 2: Input Validation & Resilience</h3>
<h4>Test Case 5: Menu Input Handling (Invalid Mode)</h4>
<p><strong>Objective:</strong> Verify the menu handles unrecognized string inputs.</p>
<ul>
	<li><strong>Expected Output:</strong> Console outputs "Unknown mode selected. Please type 'caller', 'callee', or 'exit'." and reprompts.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Typed invalid commands. The application correctly identified the unknown mode, printed the warning message, and safely prompted the user again.
	</li>
</ul>
<h4>Test Case 6: Menu Input Handling (Exit Command)</h4>
<p><strong>Objective:</strong> Verify the program can be closed cleanly via the menu.</p>
<ul>
	<li><strong>Expected Output:</strong> Console outputs "Exiting program..." and the Java process terminates gracefully.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Typed 'exit'. The loop immediately caught the exit condition and safely terminated the application.
	</li>
</ul>
<h4>Test Case 7: Non-Numeric Port Input</h4>
<p><strong>Objective:</strong> Ensure the program does not crash upon receiving letters instead of numbers for ports.</p>
<ul>
	<li><strong>Expected Output:</strong> The program catches the <code>NumberFormatException</code>, prints <code>[ERROR] Invalid input...</code>, and returns to the main menu.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Entered alphabetic characters for the port prompt. The application successfully caught the <code>NumberFormatException</code> and returned to the menu without a fatal crash.</li>
</ul>
<h4>Test Case 8: Port Collision (BindException) Handling</h4>
<p><strong>Objective:</strong> Verify the program gracefully handles attempts to bind to an already-used port.</p>
<ul>
	<li><strong>Expected Output:</strong> Terminal 2 catches the <code>BindException</code>, prints an error about the port being in use, and safely returns to the menu.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Entered alphabetic characters for the port prompt. The application successfully caught the <code>NumberFormatException</code> and returned to the menu without a fatal crash.</li>
</ul>
<h3>Phase 3: Protocol & Network Verification (Wireshark)</h3>
<h4>Test Case 9: Wireshark - SIP Handshake Sequence</h4>
<p><strong>Objective:</strong> Verify the full SIP sequence is sent over the network.</p>
<ul>
	<li><strong>Expected Output:</strong> The capture shows a distinct sequence of 4 packets: <code>INVITE</code> -&gt; <code>200 OK</code> -&gt; <code>ACK</code> -&gt; <code>BYE</code>.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Wireshark capture cleanly showed the complete SIP transaction over the Loopback Adapter. All 4 packets successfully transmitted and parsed.</li>
</ul>
<h4>Test Case 10: Wireshark - SDP Dynamic Port Negotiation</h4>
<p><strong>Objective:</strong> Verify via packet capture that the SDP payload is accurately sharing the dynamic RTP ports.</p>
<ul>
	<li><strong>Expected Output:</strong> The Message Body (SDP) of the INVITE contains <code>m=audio 9002 RTP/AVP 0</code> (Caller's RTP port). The <code>200 OK</code> contains <code>m=audio 9000 RTP/AVP 0</code> (Callee's RTP port).</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Expanded the SIP message body in Wireshark. The Media Description (<code>m=</code>) perfectly mapped the dynamic RTP ports specified in the terminal inputs for both the Caller and Callee.</li>
</ul>
<h4>Test Case 11: Wireshark - RTP Packet Structure</h4>
<p><strong>Objective:</strong> Verify that the RTP packets strictly follow the RFC 3550 standard.</p>
<ul>
	<li><strong>Expected Output:</strong> Wireshark decodes the payload type as <strong>ITU-T G.711 PCMU (0)</strong>. The Sequence Number increments by exactly 1 for each packet. The Timestamp increments by exactly 160.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Packets decoded natively as RTP. Verified the Payload Type strictly adhered to PCMU (0). Tracked sequence numbers and timestamps progressing sequentially (+1 and +160 respectively) with zero malformed packets.</li>
</ul>
<h4>Test Case 12: Wireshark - RTCP Sender Reports</h4>
<p><strong>Objective:</strong> Verify that RTCP packets are correctly formatted and transmitted periodically.</p>
<ul>
	<li><strong>Expected Output:</strong> A binary RTCP packet (Payload Type 200: Sender Report) appears exactly every 5 seconds.</li>
	<li><strong>Status:</strong> [PASSED]</li>
	<li><strong>Notes/Output:</strong> Verified. Initially saw a length of 60 bytes due to OS-level minimum Ethernet frame padding, but upon expanding the RTCP packet details in the Wireshark tree, the internal Payload Type was correctly confirmed as Sender Report (200). Packets arrive at exactly 5-second intervals.</li>
</ul>