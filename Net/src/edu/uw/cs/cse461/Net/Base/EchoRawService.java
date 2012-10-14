package edu.uw.cs.cse461.Net.Base;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.Log;

/**
 * An echo service that communicates directly over UPD and TCP sockets that it creates.
 * @author zahorjan
 *
 */
public class EchoRawService extends NetLoadableService  {
	private static final String TAG="EchoRawService";
	
	private boolean mIsUp = true;
	
	private ServerSocket mServerSocket;
	private DatagramSocket mDatagramSocket;
	
	/**
	 * A NetLoadableService must provide a public constructor taking no arguments.
	 * <p>
	 * This service must listen to both a UDP and a TCP port.  It creates sockets
	 * bound to those ports in this constructor.  It also creates a thread per socket -
	 * the thread blocks trying to receive data on its socket, and when it does,
	 * echoes back whatever it receives. 
	 * @throws Exception
	 */
	public EchoRawService() throws Exception {
		super("echoraw", true);
		
		// If there is no config file entry for the following properties, 0 is returned.
		// When the socket is created, specifying 0 for the port is a request for the OS
		// to pick any unused port.  
		int udpPort = NetBase.theNetBase().config().getAsInt("echoraw.udpport", 0, TAG);
		int tcpPort = NetBase.theNetBase().config().getAsInt("echoraw.tcpport", 0, TAG);
		
		mServerSocket = new ServerSocket(tcpPort);
		Log.i(TAG,  "Server socket port = " + mServerSocket.getLocalPort());
		
		mDatagramSocket = new DatagramSocket(udpPort);
		Log.i(TAG,  "Datagram port = " + mDatagramSocket.getLocalPort());
		
		// Code/thread handling the UDP socket
		Thread dgramThread = new Thread() {
								public void run() {
									byte buf[] = new byte[64*1024];
									DatagramPacket packet = new DatagramPacket(buf, buf.length);

									//	Thread termination in this code is primitive.  When shutdown() is called (by the
									//	application's main thread, so asynchronously to the threads just mentioned) it
									//	closes the sockets.  This causes an exception on any thread trying to read from
									//	it, which is what provokes thread termination.
									try {
										while ( mIsUp ) {
											mDatagramSocket.receive(packet);
											mDatagramSocket.send( new DatagramPacket(buf, packet.getLength(), packet.getAddress(), packet.getPort()));
										}
									} catch (Exception e) {
										Log.w(TAG,  "Dgram reading thread exiting due to exception: " + e.getMessage());
									}
								}
		};
		dgramThread.start();
		
		// Code/thread handling the TCP socket
		Thread tcpThread = new Thread() {
			public void run() {
				byte[] buf = new byte[1024];
				Socket sock = null;
				int socketTimeout = NetBase.theNetBase().config().getAsInt("echo.sockettimeout", 500, TAG);
				try {
					while ( mIsUp ) {
						// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
						// with that client.  That socket is returned.
						sock = mServerSocket.accept();
						// We're going to read from sock, to get the message to echo, but we can't risk a client mistake
						// blocking us forever.  So, arrange for the socket to give up if no data arrives for a while.
						sock.setSoTimeout(socketTimeout);
						InputStream is = sock.getInputStream();
						OutputStream os = sock.getOutputStream();
						int len;
						try {
							// keep reading until the client has closed its side of the connection
							while ( (len = is.read(buf)) >= 0 ) os.write(buf, 0, len);
						} catch (Exception e) {
							Log.i(TAG, "TCP thread done reading due to exception: " + e.getMessage());
						} finally {
							if ( sock != null ) try { sock.close(); } catch (Exception e) {}
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "TCP server thread exiting due to exception: " + e.getMessage());
				}
			}
		};
		tcpThread.start();
	}

	
	/**
	 * This method is called when the entire infrastructure
	 * wants to terminate.  We set a flag indicating all threads
	 * should terminate.  We then close the sockets.  The threads
	 * using those sockets will either timeout and see the flag set or
	 * else wake up on an IOException because the socket has been closed
	 * and notice the flag is set.  Either way, they'll terminate.
	 */
	@Override
	public void shutdown() {
		Log.d(TAG, "Shutting down");
		mIsUp = false;
		if ( mServerSocket != null ) {
			try { 
				mServerSocket.close();
			} catch (Exception e) {
				Log.e(TAG, "Couldn't close server socket: " + e.getMessage());
			}
			mServerSocket = null;
		}
		if ( mDatagramSocket != null ) {
			mDatagramSocket.close();
			mDatagramSocket = null;
		}
		
		super.shutdown();
	}
	
	/**
	 * The NetLoadableServer interface requires a method that will return a representation of
	 * the current serer state.  This server doesn't really have any state...
	 */
	@Override
	public String dumpState() {
		return loadablename() + (mIsUp ? " is up" : " is down");
	}

}
