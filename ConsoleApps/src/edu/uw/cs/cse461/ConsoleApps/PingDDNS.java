package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.ConsoleApps.PingInterface.PingDDNSInterface;
import edu.uw.cs.cse461.ConsoleApps.PingInterface.PingRPCInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.DDNS.DDNSFullName;
import edu.uw.cs.cse461.Net.DDNS.DDNSFullNameInterface;
import edu.uw.cs.cse461.Net.DDNS.DDNSRRecord.ARecord;
import edu.uw.cs.cse461.Net.DDNS.DDNSException;
import edu.uw.cs.cse461.Net.DDNS.DDNSResolverService;
import edu.uw.cs.cse461.Net.RPC.RPCCall;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public class PingDDNS extends NetLoadableConsoleApp implements PingDDNSInterface {
	private static final String TAG="PingDDNS";
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingDDNS() {
		super("pingddns", true);
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

				
				System.out.print("Enter a host name, or empty line to exit: ");
				String targetDDNS = console.readLine();
				if ( targetDDNS == null || targetDDNS.trim().isEmpty() ) return;
				

				int targetRPCPort = config.getAsInt("echorpc.port", 0, TAG);
				if ( targetRPCPort == 0 ) {
					System.out.print("Enter the server's TCP port, or empty line to skip: ");
					String targetTCPPortStr = console.readLine();
					if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) targetRPCPort = 0;
					else targetRPCPort = Integer.parseInt(targetTCPPortStr);
				}

				int nTrials = config.getAsInt("ping.ntrials", 5, TAG);
				
				System.out.println("Host: " + targetDDNS);
				System.out.println("tcp port: " + targetRPCPort);
				System.out.println("trials: " + nTrials);
				
				ElapsedTimeInterval rpcResult = null;


				rpcResult = ping(targetDDNS, nTrials);


				if ( rpcResult != null ) System.out.println("DDNS: " + String.format("%.2f msec", rpcResult.mean()));

			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			} 
		} catch (Exception e) {
			System.out.println("PingRPC.run() caught exception: " +e.getMessage());
		}
	}
	
	
	@Override
	public ElapsedTimeInterval ping(String target, int nTrials) {
		try {
			DDNSResolverService resolver = (DDNSResolverService)NetBase.theNetBase().getService("ddnsresolver");
			String name = NetBase.theNetBase().config().getProperty("net.hostname");
			if(name == null) {
				throw new Exception("No net.hostname entry in config file.");
			}
			
			DDNSFullNameInterface fullname = new DDNSFullName(name);
			
			String portString = NetBase.theNetBase().config().getProperty("ddns.rootport");
			if (portString == null) {
				throw new Exception("No ddns.rootport entry in config file.");
			}
			int port = Integer.parseInt(portString);
			
			ARecord node = resolver.resolve(target);

			for (int i = 0; i < nTrials; i++) {
				System.out.print("DDNS Trial " + (i+1) + " - ");
				
				ElapsedTime.start("PingDDNSTotal");
				try {
					JSONObject response = RPCCall.invoke(node.ip(), node.port(), "echorpc", "echo", new JSONObject());
					ElapsedTime.stop("PingDDNSTotal");
					System.out.println("SUCCESS");
				} catch (Exception e) {
					ElapsedTime.abort("PingDDNSTotal");
					System.out.println("ABORTED");
				}
	
			}
			
			return ElapsedTime.get("PingDDNSTotal");
		} catch(DDNSException e) {
			System.out.println("TRIAL FAILED: " + e.getMessage());
		} catch(JSONException e) {
			System.out.println("TRIAL FAILED: " + e.getMessage());
		} catch(Exception e) {
			System.out.println("TRIAL FAILED: " + e.getMessage());
		}
		return null;
	}


}
