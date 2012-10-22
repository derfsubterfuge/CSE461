package edu.uw.cs.cse461.Net.RPC;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.TCPMessageHandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.Log;

/**
 * Class implementing the caller side of RPC -- the RPCCall.invoke() method.
 * The invoke() method itself is static, for the convenience of the callers,
 * but this class is a normal, loadable, service.
 * <p>
 * <p>
 * This class is responsible for implementing persistent connections. 
 * (What you might think of as the actual remote call code is in RCPCallerSocket.java.)
 * Implementing persistence requires keeping a cache that must be cleaned periodically.
 * We do that using a cleaner thread.
 * 
 * @author zahorjan
 *
 */
public class RPCCall extends NetLoadableService {
	private static final String TAG="RPCCall";

	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------
	// The static versions of invoke() is just a convenience for caller's -- it
	// makes sure the RPCCall service is actually running, and then invokes the
	// the code that actually implements invoke.
	
	/**
	 * Invokes method() on serviceName located on remote host ip:port.
	 * @param ip Remote host's ip address
	 * @param port RPC service port on remote host
	 * @param serviceName Name of service to be invoked
	 * @param method Name of method of the service to invoke
	 * @param userRequest Arguments to call
	 * @return Returns whatever the remote method returns.
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject invoke(
			String ip,				  // ip or dns name of remote host
			int port,                 // port that RPC is listening on on the remote host
			String serviceName,       // name of the remote service
			String method,            // name of that service's method to invoke
			JSONObject userRequest    // arguments to send to remote method
			) throws JSONException, IOException {
		RPCCall rpcCallObj =  (RPCCall)NetBase.theNetBase().getService( "rpccall" );
		if ( rpcCallObj == null ) throw new IOException("RPCCall.invoke() called but the RPCCall service isn't loaded");
		return rpcCallObj._invoke(ip, port, serviceName, method, userRequest, true);
	}
	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------

	
	/**
	 * The infrastructure requires a public constructor taking no arguments.  Plus, we need a constructor.
	 */
	public RPCCall() {
		super("rpccall", true);
	}

	/**
	 * This private method performs the actual invocation, including the management of persistent connections.
	 * 
	 * @param ip
	 * @param port
	 * @param serviceName
	 * @param method
	 * @param userRequest
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	private JSONObject _invoke(
			String ip,				  // ip or dns name of remote host
			int port,                 // port that RPC is listening on on the remote host
			String serviceName,       // name of the remote service
			String method,            // name of that service's method to invoke
			JSONObject userRequest,   // arguments to send to remote method
			boolean tryAgain          // true if an invocation failure on a persistent connection should cause a re-try of the call, false to give up
			) throws JSONException, IOException {
		Socket socket = null;
		socket = new Socket(ip, port);
		int socketTimeout = NetBase.theNetBase().config().getAsInt("rpc.timeout", 30, TAG)*1000; //convert from seconds to millis
		socket.setSoTimeout(socketTimeout);
		TCPMessageHandler tcpMsgHandler = new TCPMessageHandler(socket);
		tcpMsgHandler.sendMessage(userRequest);
		return tcpMsgHandler.readMessageAsJSONObject();
	}
	
	/**
	 * Called when entire infrastructure is coming down.
	 */
	@Override
	public void shutdown() {
	}

	/**
	 * Called when some client wants a representation of this server's state.  
	 * (Mainly useful for debugging.)
	 */
	@Override
	public String dumpState() {
		return "There are no persistent connections.";
	}

}
