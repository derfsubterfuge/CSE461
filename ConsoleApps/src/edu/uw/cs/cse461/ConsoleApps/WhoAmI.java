package edu.uw.cs.cse461.ConsoleApps;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.RPC.RPCService;

/**
 * A ConsoleApp that prints the IP/port of the RPC service on the local machine.
 * 
 * @author zahorjan
 *
 */
public class WhoAmI extends NetLoadableConsoleApp {
	
	/**
	 * OSConsoleApp's must have a constructor taking no arguments.  The constructor can initialize,
	 * but shouldn't perform any of the function of the app.  That's done in the run() method.
	 */
	public WhoAmI() {
		super("whoami", true);
	}

	/**
	 * This method will be called each time the app is invoked (by the AppManager).
	 */
	@Override
	public void run() {
		try {
			String hostname = NetBase.theNetBase().hostname();
			if ( hostname != null ) {
				if ( hostname.isEmpty()) System.out.println("Host: root");
				else System.out.println("Host: '" + hostname + "'");
			}
			else System.out.println("Host: none");
			RPCService rpcService = (RPCService)NetBase.theNetBase().getService("rpc");
			if ( rpcService != null ) System.out.println("IP: " + rpcService.localIP() + "  Port: " + rpcService.localPort());
			else System.out.println("No RPC service is running on this node.");
		} catch (Exception e) {
			System.out.println("Caught exception: " + e.getMessage());
		}
	}
}
