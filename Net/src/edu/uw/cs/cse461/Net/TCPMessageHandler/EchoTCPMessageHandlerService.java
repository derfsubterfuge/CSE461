package edu.uw.cs.cse461.Net.TCPMessageHandler;

import java.net.ServerSocket;
import java.net.Socket;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.Log;

/**
 * An echo service that communicates directly over UPD and TCP sockets that it creates.
 * @author zahorjan
 *
 */
public class EchoTCPMessageHandlerService extends NetLoadableService  {
	private static final String TAG="EchoTCPMessageHandlerService";
	
	private boolean mIsUp = true;
	private ServerSocket mServerSocket;
	
	public EchoTCPMessageHandlerService() throws Exception {
		super("echotcpmessagehandler", true);
		
		int tcpPort = NetBase.theNetBase().config().getAsInt("echotcpmessagehandler.port", 0, TAG);
		
		mServerSocket = new ServerSocket(tcpPort);
		Log.i(TAG,  "Server socket port = " + mServerSocket.getLocalPort());
		
		Thread tcpThread = new Thread() {
			public void run() {
				try {
					while ( true ) {
						Socket sock = null;
						TCPMessageHandler tcpMessageHandlerSocket = null;
						sock = mServerSocket.accept();  // if this fails, we want out of the while loop...
						try {
							while ( true ) {
								tcpMessageHandlerSocket = new TCPMessageHandler(sock);
								String msg = tcpMessageHandlerSocket.readMessageAsString();
								tcpMessageHandlerSocket.sendMessage(msg);
							}
						} catch (Exception e) {
							Log.i(TAG, "Exception while handling connection: " + e.getMessage());
						} finally {
							if ( tcpMessageHandlerSocket != null ) tcpMessageHandlerSocket.discard();
							if ( sock != null ) try { sock.close(); } catch (Exception e) {}
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "Server thread exiting due to exception: " + e.getMessage());
				}
			}
		};
		tcpThread.start();
	}

	
	/**
	 * This method is required in every RPCCallable class.  If this object has created any 
	 * threads, it should cause them to terminate.
	 */
	@Override
	public void shutdown() {
		Log.d(TAG, "Shutting down");
		if ( mServerSocket != null ) {
			try { 
				mServerSocket.close();
			} catch (Exception e) {
				Log.e(TAG, "Couldn't close server socket: " + e.getMessage());
			}
			mServerSocket = null;
		}
		super.shutdown();
	}
	
	@Override
	public String dumpState() {
		return loadablename() + (mIsUp ? " is up" : " is down");
	}
}
