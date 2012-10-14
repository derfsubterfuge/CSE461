package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.ConfigManager;

/**
 * Raw sockets version of echo client.
 * @author zahorjan
 *
 */
public class EchoRaw extends NetLoadableConsoleApp {
	private static final String TAG="EchoRaw";
	
	/**
	 * A NetLoadableConsoleApp must have a public constructor taking no arguments.
	 * You can do in the constructor whatever is required.  Note that only one
	 * instance of the application is constructed, ever.  (It is repeatedly "invoked"
	 * for execution by calling its run() method. )
	 * <p>
	 * The superclass constructor requires two arguments.  The first is an internal
	 * name for the application - other components find this application using this
	 * name.  The second is a boolean that is set to true if the implementation is ready
	 * (enough) that the infrastructure should load it, if it's named in the config file.
	 * If that argument is false, the application won't be loaded even if named in the
	 * config file.
	 */
	public EchoRaw() {
		super("echoraw", true);
	}
	
	/**
	 * The infrastructure invokes this method when it receives a request to run
	 * this application.
	 */
	@Override
	public void run() {
		try {
			
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			// The config object represents the configuration file
			ConfigManager config = NetBase.theNetBase().config();

			// The config file may, or may not, say where the server is located
			String targetIP = config.getProperty("echoraw.server");
			if ( targetIP == null ) {
				System.out.println("No echoraw.server entry in config file.");
				System.out.print("Enter a host ip, or empty line to exit: ");
				targetIP = console.readLine();
				if ( targetIP == null || targetIP.trim().isEmpty() ) return;
			}

			// The config file may, or may not, say what udp port the server is listening on
			int targetUDPPort = config.getAsInt("echoraw.udpport", 0, TAG);
			if ( targetUDPPort == 0 ) {
				System.out.print("Enter the server's UDP port, or empty line to skip: ");
				String targetUDPPortStr = console.readLine();
				if ( targetUDPPortStr == null || targetUDPPortStr.trim().isEmpty() ) targetUDPPort = 0;
				else targetUDPPort = Integer.parseInt(targetUDPPortStr);
			}

			// The config file may, or may not, say what tcp port the server is listening on
			int targetTCPPort = config.getAsInt("echoraw.tcpport", 0, TAG);
			if ( targetTCPPort == 0 ) {
				System.out.print("Enter the server's TCP port, or empty line to skip: ");
				String targetTCPPortStr = console.readLine();
				if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) targetTCPPort = 0;
				else targetTCPPort = Integer.parseInt(targetTCPPortStr);
			}

			// read a socket timeout value from the config file, but specify a default to be returned
			// if the config file doesn't contain a value
			
			int socketTimeout = config.getAsInt("echoraw.sockettimeout", 500, TAG);
			
			while ( true ) {
				try {

					System.out.print("Enter message to be echoed, or empty string to exit: ");
					String msg = console.readLine();
					if ( msg.isEmpty() ) return;

					// skip udp connection if user insisted
					if ( targetUDPPort != 0 ) {
						DatagramSocket socket = new DatagramSocket();
						socket.setSoTimeout(socketTimeout); // wait at most 500 msec. when receiving on this socket
						byte[] buf = msg.getBytes();
						DatagramPacket packet = new DatagramPacket(buf, buf.length, new InetSocketAddress(targetIP, targetUDPPort));
						socket.send(packet);  // tell the server we're here.  The server will get our IP and port from the received packet.

						// we're supposed to get back what we sent, so the amount of buffer we need is equal to size of what we sent.
						// If the echo service makes a mistake, something bad happens. (We could check the data lenght of the received packet,
						// but we don't bother.)
						byte[] receiveBuf = new byte[buf.length];
						DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
						try { 
							socket.receive(receivePacket);
							String response = new String(receiveBuf, 0, receivePacket.getLength());
							System.out.println("UDP: '" + response + "'");
						} catch (SocketTimeoutException e) {
							// This exception is thrown if we wait on receive() longer than the timeout
							System.out.println("UDP socket timeout");
						}
						socket.close();
					}
					
					// skip tcp connection if user said to skip it
					if ( targetTCPPort != 0 ) {
						Socket tcpSocket = new Socket(targetIP, targetTCPPort);
						tcpSocket.setSoTimeout(socketTimeout);
						InputStream is = tcpSocket.getInputStream();
						OutputStream os = tcpSocket.getOutputStream();
						// send the message to be echoed back to us
						byte[] msgBytes = msg.getBytes();
						byte[] buf = new byte[msgBytes.length];
						os.write(msgBytes, 0, msgBytes.length);
						// read the reply
						try {
							int len = 0;
							while ( len < msgBytes.length) {
								len += is.read(buf, len, buf.length);
							}
							String response = new String(buf, 0, msgBytes.length);
							System.out.println("TCP: '" + response + "'");
						} catch (Exception e) {
							System.out.println("TCP read failed: " + e.getMessage());
						}
						tcpSocket.close();
					}
					
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				} 
			}
		} catch (Exception e) {
			System.out.println("Echo.run() caught exception: " +e.getMessage());
		}
	}
}
