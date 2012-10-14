package edu.uw.cs.cse461.Net.Base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.Log;

/**
 * Transfers reasonably large amounts of data to client over raw TCP and UDP sockets.  In both cases,
 * the server simply sends as fast as it can.  The server does not implement any correctness mechanisms,
 * so using UDP clients may not receive all the data sent.
 * <p>
 * Four consecutive ports are used to send fixed amounts of data of various sizes.
 * <p>
 * @author zahorjan
 *
 */
public class DataXferRawService extends NetLoadableService  {
	private static final String TAG="DataXferRawService";
	
	public static final int NPORTS = 4;
	public static final int[] XFERSIZE = {1000, 10000, 100000, 1000000};

	private int mBasePort;
	
	private boolean mIsUp = true;
	
	private ServerSocket[] mServerSockets = new ServerSocket[NPORTS];
	private DatagramSocket[] mDatagramSockets = new DatagramSocket[NPORTS];
	
	public DataXferRawService() throws Exception {
		super("dataxferraw", true);
		
		ConfigManager config = NetBase.theNetBase().config();
		mBasePort = config.getAsInt("dataxferraw.baseport", 0, TAG);
		if ( mBasePort == 0 ) throw new RuntimeException("dataxferraw service can't run -- no dataxferraw.baseport entry in config file");
		
		for(int i = 0; i < NPORTS; i++) {
			
			mServerSockets[i] = new ServerSocket(mBasePort + i);
			Log.i(TAG,  "Server socket port = " + mServerSockets[i].getLocalPort());
			(new TCPThread(mServerSockets[i])).start();
			
			
			mDatagramSockets[i] = new DatagramSocket(mBasePort + i);
			Log.i(TAG,  "Datagram socket port = " + mDatagramSockets[i].getLocalPort());
			(new UDPThread(mDatagramSockets[i])).start();
			
		}
		
	}

	
	/**
	 * This method is required in every RPCCallable class.  If this object has created any 
	 * threads, it should cause them to terminate.
	 */
	@Override
	public void shutdown() {
		Log.d(TAG, "Shutting down");
		mIsUp = false;
		for(int i = 0; i < NPORTS; i++) {
			if ( mServerSockets[i] != null ) {
				try { 
					mServerSockets[i].close();
				} catch (Exception e) {
					Log.e(TAG, "Couldn't close server socket at local port " + (mBasePort+i) + ": " + e.getMessage());
				}
				mServerSockets[i] = null;
			}
			
			if ( mDatagramSockets[i] != null ) {
				mDatagramSockets[i].close();
				mDatagramSockets[i] = null;
			}
		}
		super.shutdown();
	}
	
	@Override
	public String dumpState() {
		return loadablename() + (mIsUp ? " is up" : " is down");
	}
	
	private class TCPThread extends Thread {
		
		public ServerSocket mServerSocket;
		private final int MAX_BYTES_SENT = 1024;
		
		public TCPThread(ServerSocket socket) {
			if(socket == null)
				throw new IllegalArgumentException("TCP Socket cannot be null");
			mServerSocket = socket;
		}
		
		public void run() {
			Socket socket = null;
			
			int socketTimeout = NetBase.theNetBase().config().getAsInt("dataxferraw.sockettimeout", 500, TAG);
			try {
				while ( mIsUp ) {
					// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
					// with that client.  That socket is returned.
					socket = mServerSocket.accept();
					
					// We're going to read from sock, to get the message to echo, but we can't risk a client mistake
					// blocking us forever.  So, arrange for the socket to give up if no data arrives for a while.
					socket.setSoTimeout(socketTimeout);
					OutputStream os = socket.getOutputStream();
					
					try {
						int bytesSent = 0;
						int totalBytesToSend = XFERSIZE[mServerSocket.getLocalPort() - mBasePort];
						while(bytesSent < totalBytesToSend) {
							int bytesToSend = Math.min(MAX_BYTES_SENT, totalBytesToSend - bytesSent);
							os.write(new byte[bytesToSend]);
							bytesSent += bytesToSend;
						}
					} catch (Exception e) {
						Log.i(TAG, "TCP thread done reading due to exception: " + e.getMessage());
					} finally {
						if ( socket != null ) try { socket.close(); } catch (Exception e) {}
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "TCP server thread exiting due to exception: " + e.getMessage());
			}
		}
	}
	
	private class UDPThread extends Thread {
		
		public DatagramSocket mDatagramSocket;
		private final int MAX_BYTES_SENT = 1000;
		
		public UDPThread(DatagramSocket socket) {
			if(socket == null)
				throw new IllegalArgumentException("UDP Socket cannot be null");
			mDatagramSocket = socket;
		}
		
		public void run() {
			int totalBytesToSend = XFERSIZE[mDatagramSocket.getLocalPort() - mBasePort];
			byte[] buf = new byte[MAX_BYTES_SENT];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				while ( mIsUp ) {
					mDatagramSocket.receive(packet);
					int bytesSent = 0;
					while(bytesSent < totalBytesToSend) {
						int bytesToSend = Math.min(MAX_BYTES_SENT, totalBytesToSend - bytesSent);
						mDatagramSocket.send( new DatagramPacket(new byte[bytesToSend], bytesToSend, packet.getAddress(), packet.getPort()));
						bytesSent += bytesToSend;
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "UDP server thread exiting due to exception: " + e.getMessage());
			}
		}
	}
}
