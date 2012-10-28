package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.json.JSONObject;

import edu.uw.cs.cse461.ConsoleApps.DataXferInterface.DataXferRPCInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.RPC.RPCCall;
import edu.uw.cs.cse461.util.Base64;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public class DataXferRPC extends NetLoadableConsoleApp implements DataXferRPCInterface {
	private static final String TAG="DataXferRPC";
	
	// ConsoleApp's must have a constructor taking no arguments
	public DataXferRPC() {
		super("dataxferrpc", true);
	}
	
	@Override
	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			ConfigManager config = NetBase.theNetBase().config();

			String targetIP = config.getProperty("dataxferrpc.server");
			if ( targetIP == null ) {
				System.out.println("No echorpc.server entry in config file.");
				System.out.print("Enter a host ip, or empty line to exit: ");
				targetIP = console.readLine();
				if ( targetIP == null || targetIP.trim().isEmpty() ) return;
			}

			int targetRPCPort = config.getAsInt("dataxferrpc.port", 0, TAG);
			if ( targetRPCPort == 0 ) {
				System.out.print("Enter the server's TCP port, or empty line to exit: ");
				String targetTCPPortStr = console.readLine();
				if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) return;
				else targetRPCPort = Integer.parseInt(targetTCPPortStr);
			}
			
			int nTrials = config.getAsInt("dataxferrpc.ntrials", -1, TAG);
			if ( nTrials == -1 ) {
				System.out.print("Enter number of trials: ");
				String trialStr = console.readLine();
				nTrials = Integer.parseInt(trialStr);
			}
			
			while ( true ) {
				try {
					int len = -1;
					String msg = null;
					while(len < 0) {
						try {
							System.out.print("Enter requested transfer size (positive integer), or a negative integer to exit: ");
							msg = console.readLine();
							len = Integer.parseInt(msg);
							if(len < 0)
								return;
						} catch(NumberFormatException e) {
							System.out.println("Error: '" + msg + "' is not an integer.  Please try again.");
						}
					}
					
					TransferRateInterval rpcStats = DataXfer(targetIP, targetRPCPort, len, nTrials);
					
					System.out.println("\nRPC: xfer rate = " + String.format("%9.0f", rpcStats.mean() * 1000.0) + " bytes/sec.");
					System.out.println("RPC: failure rate = " + String.format("%5.1f", rpcStats.failureRate()) +
							           " [" + rpcStats.nAborted()+ "/" + rpcStats.nTrials() + "]");
					
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				} 
			}
		} catch (Exception e) {
			System.out.println("Echo.run() caught exception: " +e.getMessage());
		}
	}

	@Override
	public TransferRateInterval DataXfer(String hostIP, int port,
			int xferLength, int nTrials) throws Exception {
		
		for(int i = 0; i < nTrials; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

			TransferRate.start("dataxferrpc");
			JSONObject response = RPCCall.invoke(hostIP, port, "dataxferrpc", "dataxfer", new JSONObject().put("xferLength", xferLength) );
			if(response.has("data")) {
				byte[] data = Base64.decode(response.getString("data"));
				if(data.length == xferLength) {
					TransferRate.stop("dataxferrpc", data.length);
					System.out.println("All data recieved!");
					
				} else {
					TransferRate.abort("dataxferrpc", data.length);
					System.out.println("Only recieved " + data.length + " of " + xferLength + " bytes");
				}
			} else {
				TransferRate.stop("dataxferrpc", 0);
				System.out.println("No data returned!?");
			}
		}
		return TransferRate.get("dataxferrpc");
	}
}