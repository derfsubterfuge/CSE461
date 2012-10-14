package edu.uw.cs.cse461.Net.RPC;

import java.net.UnknownHostException;

/**
 * The interface for the side of RPC that receives incoming calls.
 * @author zahorjan
 *
 */
public interface RPCServiceInterface {

	public void registerHandler(String serviceName, String methodName, RPCCallableMethod method) throws Exception;
	
	public String localIP() throws UnknownHostException;
	public int localPort();
	
}
