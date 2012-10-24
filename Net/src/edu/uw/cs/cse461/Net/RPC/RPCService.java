package edu.uw.cs.cse461.Net.RPC;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

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
	private static final int MAX_READ_SIZE = 10000; //bytes
	private Map<String, RPCCallableMethod> mServiceMethodMap = new HashMap<String, RPCCallableMethod>();
	private ServerSocket mServerSocket = null;
	private boolean mIsUp = false;
	private static int mId = 0;
	
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
		// create serversocket, binding to port from config file, or 0 if there isn't one given
		int rpcPort = NetBase.theNetBase().config().getAsInt("rpc.serverport", 0, TAG);
		mServerSocket = new ServerSocket(rpcPort);
		Log.i(TAG,  "Server socket port = " + mServerSocket.getLocalPort());
		mIsUp = true;
		
		// start listening for connections on this serversocket
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
				Log.e(TAG, "Error shutting down RPCService.");
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
	
	private synchronized int incId() {
		return mId++;
	}
	
	private void sendErrorMsg(int callid, TCPMessageHandler msgHandler, String message) {
		try {
			JSONObject errorMsg = new JSONObject();
			errorMsg.put("id", mId);
			errorMsg.put("host", localIP());
			errorMsg.put("callid", callid);
			errorMsg.put("type", "ERROR");
			errorMsg.put("msg", message);
			msgHandler.sendMessage(errorMsg);
		} catch (Exception e) {
			// do nothing - if we run into an error while trying to send an error message there's not much we can do
		}
	}
	
	private class RPCListenThread extends Thread {
		
		public void run() {
			Socket socket = null;
			
			int socketTimeout = NetBase.theNetBase().config().getAsInt("rpc.timeout", 30, TAG)*1000; //convert from seconds to millis
			try {
				while ( mIsUp ) {
					// accept() blocks until a client connects.  When it does, a new socket is created that communicates only
					// with that client.  That socket is returned.
					try {
						socket = mServerSocket.accept();
					
						socket.setSoTimeout(socketTimeout);
						(new RPCWorkThread(socket)).start();
						
						//make null so that this doesn't kill the spawned threads if an exception occurs
						socket = null;
					} catch(IOException e) {
						Log.e(TAG, "Was unable to establish I/O connection.");
						
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
	
	
	// the RPCWorkThread is responsible for interacting with the caller
	// (handshake --> rpccall --> close socket and thread dies)
	private class RPCWorkThread extends Thread {
		Socket mSocket = null;
		
		public RPCWorkThread(Socket socket) {
			mSocket = socket;
		}
		
		public void run() {
			TCPMessageHandler tcpMsgHandler = null;
			int callId = 0;
			try {
				incId();
				
				tcpMsgHandler = new TCPMessageHandler(mSocket);
				tcpMsgHandler.setMaxReadLength(MAX_READ_SIZE);
				
				//TODO: replace all strings with constants in TCPMessageHandler
				
				// receive and validate the handshake "connect" request from the caller
				JSONObject handshake = tcpMsgHandler.readMessageAsJSONObject();
				callId = handshake.getInt("id");
				if(!handshake.getString("action").equals("connect")) {
					sendErrorMsg(callId, tcpMsgHandler, "Initial message did not have the action 'connect'");
					return;
				}
				if(!handshake.getString("type").equals("control")) {
					sendErrorMsg(callId, tcpMsgHandler,"Initial message did not have type 'control'");
					return;
				}
				
				// if we've reached here, then we're willing to connect - return the handshake
				JSONObject returnShake = new JSONObject();
				returnShake.put("id", mId);
				returnShake.put("host", localIP());
				returnShake.put("type", "OK");
				returnShake.put("callid", callId);
				
				// send the handshake back to the caller
				tcpMsgHandler.sendMessage(returnShake);
				
				// now read the rpccall request from the caller
				JSONObject request = tcpMsgHandler.readMessageAsJSONObject();
				// make sure this is a method invocation request
				if(!request.getString("type").equals("invoke")) {
					sendErrorMsg(callId, tcpMsgHandler,"RPCCall request message did not have type 'invoke'.");
					return;
				}
				// now get all the info we need to invoke the method
				String app = request.getString("app");
				String method = request.getString("method");
				JSONObject args = request.getJSONObject("args");
				
				// validate the invocation request:
				if(!mServiceMethodMap.containsKey(app+"."+method)) {
					sendErrorMsg(callId, tcpMsgHandler, "The requested method is not registered - unable to invoke");
					return;
				}
				
				// attempt to invoke the requested method 
				JSONObject returnObj = null;
				try {
					returnObj = mServiceMethodMap.get(app+"."+method).handleCall(args);
				} catch (Exception e) {
					// create and send back error response for method invocation: (different from handshake errormsg!)
					JSONObject methodErrMsg = new JSONObject();
					methodErrMsg.put("id", mId);
					methodErrMsg.put("host", localIP());
					methodErrMsg.put("callid", callId);
					methodErrMsg.put("type", "ERROR");
					methodErrMsg.put("message", e.toString());
					methodErrMsg.put("callargs", request);
					tcpMsgHandler.sendMessage(methodErrMsg);
					return;
				}
				
				// if there were no problems return response
				// shouldn't have to worry about returnObj being null here, 
				// unless that's what the method wanted to return
				JSONObject response = new JSONObject();
				response.put("id", mId);
				response.put("host", localIP());
				response.put("callid", callId);
				response.put("value", returnObj);
				response.put("type", "OK");
				
				tcpMsgHandler.sendMessage(response);

			} catch (JSONException e) {
				sendErrorMsg(callId, tcpMsgHandler, e.toString());
			} catch (IOException e) {
				sendErrorMsg(callId, tcpMsgHandler, e.toString());
			} catch (Exception e) {
				sendErrorMsg(callId, tcpMsgHandler, e.toString());
			} finally {
				// discard tcpMsgHandler and close socket
				if(tcpMsgHandler != null) {
					try {
						tcpMsgHandler.discard();
					} catch(Exception e1) {
						//shouldn't happen
					}
				}
				if(mSocket != null) {
					try { 
						mSocket.close();
					} catch (Exception e1) {
						//shouldn't happen
					}
				}	
			}

		}
	}
}
