package edu.uw.cs.cse461.Net.RPC;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.Base64;

/**
 * A simple service that simply echoes back whatever it is sent.
 * It exposes a single method via RPC: echo.
 * <p>
 * To make a method available via RPC you must do two key things:
 * <ol>
 * <li> Create an <tt>RPCCallableMethod</tt> object that describes the method
 *      you want to expose.  In this class, that's done with two
 *      statements:
 *      <ol>
 *      <li><tt>private RPCCallableMethod<EchoService> echo;</tt>
 *      <br>declares a variable that can hold a method description
 *      of the type the infrastructure requires to invoke a method.
 *      <li><tt>echo = new RPCCallableMethod<EchoService>(this, "_echo");</tt>
 *      <br>initializes that variable.  The arguments mean that the method to
 *      invoke is <tt>this->_echo()</tt>.
 *      </ol>
 *  <p>
 *  <li> Register the method with the RPC service:
 *       <br><tt>((RPCService)OS.getService("rpc")).registerHandler(servicename(), "echo", echo );</tt>
 *       <br>This means that when an incoming RPC specifies service "echo" (the 1st argument)
 *       and method "echo" (the 2nd), that the method described by RPCCallableMethod variable
 *       <tt>echo</tt> should be invoked.
 * </ol>
 * @author zahorjan
 *
 */
public class DataXferRPCService extends NetLoadableService  {
	
	private boolean isUp = true;

	// A variable capable of describing a method that can be invoked by RPC.
	private RPCCallableMethod echo;
	
	/**
	 * The constructor registers RPC-callable methods with the RPCService.
	 * @throws IOException
	 * @throws NoSuchMethodException
	 */
	public DataXferRPCService() throws Exception {
		super("dataxferrpc", true);
		
		// Set up the method descriptor variable to refer to this->_echo()
		echo = new RPCCallableMethod(this, "_dataxfer");
		// Register the method with the RPC service as externally invocable method "echo"
		((RPCService)NetBase.theNetBase().getService("rpc")).registerHandler(loadablename(), "dataxfer", echo );
	}
	
	/**
	 * This method is required in every RPCCallable class.  If this object has created any 
	 * threads, it should cause them to terminate.
	 */
	@Override
	public void shutdown() {
		isUp = false;
		super.shutdown();
	}
	
	@Override
	public String dumpState() {
		return loadablename() + (isUp ? " is up" : " is down");
	}
	
	/**
	 * This method is callable by RPC (because of the actions taken by the constructor).
	 * <p>
	 * All RPC-callable methods take a JSONObject as their single parameter, and return
	 * a JSONObject.  (The return value can be null.)  This particular method simply
	 * echos its arguments back to the caller. 
	 * @param args
	 * @return
	 * @throws JSONException
	 */
	public JSONObject _dataxfer(JSONObject args) throws JSONException, IOException {
		// We can't assume the underlying implementation won't modify args in some way that is
		// incompatible with return value, so have to make a copy of the args.
		
		// ANDROID INCOMPATIBILITY
		//JSONObject result = new JSONObject(args, JSONObject.getNames(args));
		int len = args.getInt("xferLength");
		if(len < 0) {
			throw new IllegalArgumentException("xferLength cannot be negative");
		}
		
		JSONObject result = new JSONObject();
		result.put("data", Base64.encodeBytes(new byte[len]));
		return result;
	}
}
