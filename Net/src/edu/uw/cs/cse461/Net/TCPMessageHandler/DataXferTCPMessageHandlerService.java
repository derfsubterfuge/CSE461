package edu.uw.cs.cse461.Net.TCPMessageHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.Log;

public class DataXferTCPMessageHandlerService extends NetLoadableService {
	private static final String TAG="DataXferTCPMessageHandlerService";

	public static final int MAX_SINGLE_TRANSFER_SIZE = 1000; //bytes

	private int mPort;
	
	private boolean mIsUp = true;
	
	private ServerSocket mServerSocket = null;
	
	public DataXferTCPMessageHandlerService() throws Exception {
		super("dataxfertcpmessagehandler", true);
		
		ConfigManager config = NetBase.theNetBase().config();
		mPort = config.getAsInt("dataxfertcpmessagehandler.port", 0, TAG);
		if ( mPort == 0 ) throw new RuntimeException("dataxfertcpmessagehandler service can't run -- no dataxfertcpmessagehandler.port entry in config file");
			
		mServerSocket = new ServerSocket(mPort);
		Log.i(TAG,  "Server socket port = " + mServerSocket.getLocalPort());
		(new TCPThread()).start();
	}

	/**
	 * This method is required in every RPCCallable class.  If this object has created any 
	 * threads, it should cause them to terminate.
	 */
	public void shutdown() {
		Log.d(TAG, "Shutting down");
		mIsUp = false;
		
		if ( mServerSocket != null ) {
			try { 
				mServerSocket.close();
			} catch (Exception e) {
				Log.e(TAG, "Couldn't close server socket at local port " + (mPort) + ": " + e.getMessage());
			}
			mServerSocket = null;
		}
		
		super.shutdown();
	}
	
	public String dumpState() {
		return loadablename() + (mIsUp ? " is up" : " is down");
	}

	private class TCPThread extends Thread {
		
		public void run() {
			Socket socket = null;
			
			int socketTimeout = NetBase.theNetBase().config().getAsInt("dataxfertcpmessagehandler.sockettimeout", 500, TAG);
			try {
				while ( mIsUp ) {
					// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
					// with that client.  That socket is returned.
					socket = mServerSocket.accept();
					try {
						socket.setSoTimeout(socketTimeout);
						TCPMessageHandler tcpMsgHandler = new TCPMessageHandler(socket); 
						tcpMsgHandler.setMaxReadLength(250);
					
						JSONObject sizeMessage = tcpMsgHandler.readMessageAsJSONObject();
						int totalBytesToSend = sizeMessage.getInt(TCPMessageHandler.TRANSFER_SIZE_KEY);
						
						while(totalBytesToSend > 0) {
							int bytesToSend = Math.min(MAX_SINGLE_TRANSFER_SIZE, totalBytesToSend);
							tcpMsgHandler.sendMessage(new byte[bytesToSend]);
							totalBytesToSend -= bytesToSend;
						}
					} catch(JSONException e) {
						Log.e(TAG, "Did not recieve a complete JSONArray with the transfer size");
					} catch(IOException e) {
						Log.e(TAG, "Was unable to establish I/O connection.");
					} finally {
						if ( socket != null ) try { socket.close(); } catch (Exception e) {}
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "TCP server thread exiting due to exception: " + e.getMessage());
			}
		}
	}
}
