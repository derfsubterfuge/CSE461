package edu.uw.cs.cse461.Net.RPC;

import java.io.IOException;
import java.net.Socket;

import org.json.JSONException;

/**
 * Implements a Socket to use in sending remote RPC invocations.  (It must engage
 * in the RPC handshake before sending the invocation request.)
 * @author zahorjan
 *
 */
 class RPCCallerSocket extends Socket {
	private static final String TAG = "RPCCallerSocket";
	
	/**
	 * This constructor is here just so this class file with have some code, to avoid
	 * Java or Eclipse complaining.  You should make the constructor whatever you want.
	 * This class is used only by this package (and, in particular, by RCPCall).  
	 * The only public inteface is that supported by RPCCall, so you don't have to have this
	 * class at all if you don't want it.
	 */
	RPCCallerSocket(String hostname, String ip, int port, boolean wantPersistent) throws IOException, JSONException {
		super(ip, port);
	}

	/**
	 * Close this socket.
	 */
	@Override
	synchronized public void close() throws IOException {
	}
	
}
