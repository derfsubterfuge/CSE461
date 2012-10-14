package edu.uw.cs.cse461.Net.RPC;

import java.net.UnknownHostException;

import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.IPFinder;

/**
 * Implements the server side of RPC that receives remote invocation requests.
 * 
 * @author zahorjan
 *
 */
public class RPCService extends NetLoadableService implements RPCServiceInterface {
	private static final String TAG="RPCService";
	
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
	}
	
	/**
	 * System is shutting down imminently.  Do any cleanup required.
	 */
	@Override
	public void shutdown() {
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
		return 0;
	}
	
	@Override
	public String dumpState() {
		return "some string";
	}
	
}
