package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONObject;

import edu.uw.cs.cse461.ConsoleApps.PingInterface.PingRPCInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.RPC.RPCCall;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public class PingRPC extends NetLoadableConsoleApp implements PingRPCInterface {
	private static final String TAG="PingRaw";
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingRPC() {
		super("pingrpc", true);
	}
	
	/* (non-Javadoc)
	 * @see edu.uw.cs.cse461.ConsoleApps.PingInterface#run()
	 */
	@Override
	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			ConfigManager config = NetBase.theNetBase().config();

			try {

				ElapsedTime.clear();

				String targetIP = config.getProperty("echorpc.server");
				if ( targetIP == null ) {
					System.out.println("No echorpc.server entry in config file.");
					System.out.print("Enter a host ip, or empty line to exit: ");
					targetIP = console.readLine();
					if ( targetIP == null || targetIP.trim().isEmpty() ) return;
				}

				int targetRPCPort = config.getAsInt("echorpc.port", 0, TAG);
				if ( targetRPCPort == 0 ) {
					System.out.print("Enter the server's TCP port, or empty line to skip: ");
					String targetTCPPortStr = console.readLine();
					if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) targetRPCPort = 0;
					else targetRPCPort = Integer.parseInt(targetTCPPortStr);
				}

				int nTrials = config.getAsInt("ping.ntrials", 5, TAG);
				
				System.out.println("Host: " + targetIP);
				System.out.println("tcp port: " + targetRPCPort);
				System.out.println("trials: " + nTrials);
				
				ElapsedTimeInterval rpcResult = null;

				if ( targetRPCPort != 0 ) {
					rpcResult = ping(targetIP, targetRPCPort, nTrials);
				}

				if ( rpcResult != null ) System.out.println("RPC: " + String.format("%.2f msec", rpcResult.mean()));

			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			} 
		} catch (Exception e) {
			System.out.println("PingRPC.run() caught exception: " +e.getMessage());
		}
	}
	
	
	@Override
	public ElapsedTimeInterval ping(String targetIP, int rpcPort, int nTrials) throws Exception {
		for (int i = 0; i < nTrials; i++) {
			System.out.println("RPC Trial " + (i+1));
			boolean aborted = false;
			
			ElapsedTime.start("PingRPCTotal");
			try {
				JSONObject response = RPCCall.invoke(targetIP, rpcPort, "echorpc", "echo", new JSONObject());
			} catch (Exception e) {
				aborted = true;
				ElapsedTime.abort("PingRPCTotal");
			}
			if (!aborted) {
				ElapsedTime.stop("PingRPCTotal");
			}


		}
		return ElapsedTime.get("PingRPCTotal");
	}


}
