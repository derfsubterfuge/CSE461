package edu.uw.cs.cse461.Net.RPC;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.TCPMessageHandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * Implements the server side of RPC that receives remote invocation requests.
 * 
 * @author zahorjan
 *
 */
public class RPCService extends NetLoadableService implements RPCServiceInterface {
	private static final String TAG="RPCService";
	private Map<String, RPCCallableMethod> mServiceMethodMap = new HashMap<String, RPCCallableMethod>();
	private ServerSocket mServerSocket = null;
	private boolean mIsUp = false;
	/**
	 * Constructor.  Creates the Java ServerSocket and binds it to a port.
	 * If the config file specifies an rpc.serverport value, it should be bound to that port.
	 * Otherwise, you should specify port 0, meaning the operating system should choose a currently unused port.
	 * (The config file settings are available via the OS object.)
	 * <p>
	 * Once the port is created, a thread needs to be created to listen for connections on it.
	 * 
	 * @throws Exception
	 */
	public RPCService() throws Exception {
		super("rpc", true);
		Log.i(TAG,  "Server socket port = " + mServerSocket.getLocalPort());
		mIsUp = true;
		(new RPCListenThread()).start();
	}
	
	/**
	 * System is shutting down imminently.  Do any cleanup required.
	 */
	@Override
	public void shutdown() {
		if(mServerSocket != null) {
			try {
				mIsUp = false;
				mServerSocket.close();
			} catch (IOException e) {
				//Shouldn't happen
			}
			
		}
	}
	
	/**
	 * Services and applications with RPC callable methods register them with the RPC service using this routine.
	 * Those methods are then invoked as callbacks when an remote RPC request for them arrives.
	 * @param serviceName  The name of the service.
	 * @param methodName  The external, well-known name of the service's method to call
	 * @param method The descriptor allowing invocation of the Java method implementing the call
	 * @throws Exception
	 */
	@Override
	public synchronized void registerHandler(String serviceName, String methodName, RPCCallableMethod method) throws Exception {
		String key = serviceName + "." + methodName;
		if(mServiceMethodMap.containsKey(key))
			throw new Exception(key + " is already registered");
		mServiceMethodMap.put(key, method);
	}
	
	/**
	 * Returns the local IP address.
	 * @return
	 * @throws UnknownHostException
	 */
	@Override
	public String localIP() throws UnknownHostException {
		return IPFinder.getMyIP();
	}

	/**
	 * Returns the port to which the RPC ServerSocket is bound.
	 * @return
	 */
	@Override
	public int localPort() {
		if(mServerSocket != null)
			return mServerSocket.getLocalPort();
		return -1;
	}
	
	@Override
	public String dumpState() {
		return loadablename() + (mIsUp ? " is up" : " is down");
	}
	
	private class RPCListenThread extends Thread {
		
		public void run() {
			Socket socket = null;
			TCPMessageHandler tcpMsgHandler = null;
			
			int socketTimeout = NetBase.theNetBase().config().getAsInt("rpc.timeout", 30, TAG)*1000; //convert from seconds to millis
			try {
				while ( mIsUp ) {
					// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
					// with that client.  That socket is returned.
					try {
						socket = mServerSocket.accept();
					
						socket.setSoTimeout(socketTimeout);
						tcpMsgHandler = new TCPMessageHandler(socket);
						(new RPCWorkThread(socket, tcpMsgHandler)).start();
						
						//make null so that this doesn't kill the spawned threads if an exception occurs
						socket = null;
						tcpMsgHandler = null;
					} catch(IOException e) {
						Log.e(TAG, "Was unable to establish I/O connection.");
						
						if(tcpMsgHandler != null) {
							try {
								tcpMsgHandler.discard();
							} catch(Exception e1) {
								//shouldn't happen
							}
						}
						
						if(socket != null) {
							try { 
								socket.close();
							} catch (Exception e1) {
								//shouldn't happen
							}
						}
					}
				}
			} catch (Exception e) {
				Log.w(TAG, "RPC server thread exiting due to exception: " + e.getMessage());
			}
		}
	}
	
	private class RPCWorkThread extends Thread {
		TCPMessageHandler mTcpMsgHandler = null;
		Socket mSocket = null;
		
		public RPCWorkThread(Socket socket, TCPMessageHandler tcpMsgHandler) {
			mTcpMsgHandler = tcpMsgHandler;
			mSocket = socket;
		}
		
		public void run() {
			
		}
	}
}
