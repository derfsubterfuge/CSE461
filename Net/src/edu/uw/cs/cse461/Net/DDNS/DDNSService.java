package edu.uw.cs.cse461.Net.DDNS;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.HTTP.HTTPProviderInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.DDNS.DDNSException.DDNSRuntimeException;
import edu.uw.cs.cse461.Net.RPC.RPCCallableMethod;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.util.Log;

/**
 * Protocol: Based on RPC.  The calls:
 * <p>
 * Request:  method: "register" 
 *           args: 
 * <br>Response:  void
 * <p>
 * Fetch all records (for all apps) for a specific host.
 * Request:  method: "fetchall"
 *           args:  {host: hostname}
 * <br>Response:  [ [appname, port, authoritative], ...]
 *
 * <pre>
 * app:"ddns" supports RPC calls:
 *     register( {host: hostname,  ip: ipaddr,   port: portnum} ) => { status: "OK" } or errormsg
 *     resolve( { host: hostname } ) => { host: repeats hostname, ip: ip address, authoritative: boolean } ) or errormsg
 * </pre>
 * 
 *  * @author zahorjan
 *
 */
public class DDNSService extends NetLoadableService implements HTTPProviderInterface, DDNSServiceInterface {
	private static String TAG="DDNSService";
	
	private RPCCallableMethod resolve;
	private RPCCallableMethod register;
	private RPCCallableMethod unregister;

	/**
	 * Called to end execution.  Specifically, need to terminate any threads we've created.
	 */
	@Override
	public void shutdown() {
		super.shutdown();
	}
	
	@Override
	public String httpServe(String[] uriArray) { return toString();	}
	
	/**
	 * Constructor.  Registers the system RPCServerSocket with the parent as
	 * this host's ip address.  Registers the root server and itself in the
	 * local name cache.
	 * @throws DDNSException
	 */
	public DDNSService() throws DDNSException {
		super("ddns", true);
		
		try {
			//--------------------------------------------------------------
			// set up RPC callable methods
			//--------------------------------------------------------------

			// export methods via the rpc service
			resolve = new RPCCallableMethod(this, "_rpcResolve");
			register = new RPCCallableMethod(this, "_rpcRegister");
			unregister = new RPCCallableMethod(this, "_rpcUnregister");

			RPCService rpcService = (RPCService)NetBase.theNetBase().getService("rpc");
			rpcService.registerHandler(loadablename(), "register", register );
			rpcService.registerHandler(loadablename(), "unregister", unregister );
			rpcService.registerHandler(loadablename(), "resolve", resolve );
			
		} catch (Exception e) {
			String msg = "DDNSService constructor caught exception: " + e.getMessage();
			Log.e(TAG, msg);
			e.printStackTrace();
			throw new DDNSRuntimeException(msg);
		}
	}
	
	//---------------------------------------------------------------------------
	// RPC callable routines
	
	/**
	 * Indicates host is going offline.
	 *      unregister( {name: name, password: password} ) => { status: "OK" } or errormsg
	 * @param args
	 * @return
	 * @throws JSONException
	 * @throws DDNSException
	 */
	public JSONObject _rpcUnregister(JSONObject args) {
		return null;
	}
	
	/**
	*   register( {name: <string>, password: <string>, ip: <string>,  port: <int>} ) => { DDNSNode } or errormsg
	*<p>
	* We accept only requests for names stored on this server.
	* 
	* @param args
	* @return
	*/
	public JSONObject _rpcRegister(JSONObject args) {
		return null;
	}
	
	/**
	 * This version is invoked via RPC.  It's simply a wrapper that extracts the call arguments
	 * and invokes resolve(host).
	 * @param callArgs
	 * @return
	 */
	public JSONObject _rpcResolve(JSONObject args) {
		return null;
	}
	
	// RPC callable routines
	//---------------------------------------------------------------------------

	@Override
	public String dumpState() {
		return "whatever you'd like";
	}

}
