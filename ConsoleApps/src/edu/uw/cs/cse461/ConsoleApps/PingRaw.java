package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.uw.cs.cse461.ConsoleApps.PingInterface.PingRawInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

/**
 * Raw sockets version of ping client.
 * @author zahorjan
 *
 */
public class PingRaw extends NetLoadableConsoleApp implements PingRawInterface {
	private static final String TAG="PingRaw";
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingRaw() {
		super("pingraw", true);
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

				String targetIP = config.getProperty("echoraw.server");
				if ( targetIP == null ) {
					System.out.println("No echoraw.server entry in config file.");
					System.out.print("Enter a host ip, or empty line to exit: ");
					targetIP = console.readLine();
					if ( targetIP == null || targetIP.trim().isEmpty() ) return;
				}

				int targetUDPPort = config.getAsInt("echoraw.udpport", 0, TAG);
				if ( targetUDPPort == 0 ) {
					System.out.print("Enter the server's UDP port, or empty line to skip: ");
					String targetUDPPortStr = console.readLine();
					if ( targetUDPPortStr == null || targetUDPPortStr.trim().isEmpty() ) targetUDPPort = 0;
					else targetUDPPort = Integer.parseInt(targetUDPPortStr);
				}

				int targetTCPPort = config.getAsInt("echoraw.tcpport", 0, TAG);
				if ( targetTCPPort == 0 ) {
					System.out.print("Enter the server's TCP port, or empty line to skip: ");
					String targetTCPPortStr = console.readLine();
					if ( targetTCPPortStr == null || targetTCPPortStr.trim().isEmpty() ) targetTCPPort = 0;
					else targetTCPPort = Integer.parseInt(targetTCPPortStr);
				}

				int nTrials = config.getAsInt("ping.ntrials", 5, TAG);
				int socketTimeout = config.getAsInt("ping.sockettimeout", 500, TAG);
				
				System.out.println("Host: " + targetIP);
				System.out.println("udp port: " + targetUDPPort);
				System.out.println("tcp port: " + targetTCPPort);
				System.out.println("trials: " + nTrials);
				
				ElapsedTimeInterval udpResult = null;
				ElapsedTimeInterval tcpResult = null;

				if ( targetUDPPort != 0  ) {
					// we rely on knowing the implementation of udpPing here -- we throw
					// away the return value because we'll print the ElaspedTime stats
					udpResult = udpPing(targetIP, targetUDPPort, socketTimeout, nTrials);
				}

				if ( targetTCPPort != 0 ) {
					tcpResult = tcpPing(targetIP, targetTCPPort, socketTimeout, nTrials);
				}

				if ( udpResult != null ) System.out.println("UDP: " + String.format("%.2f msec", udpResult.mean()));
				if ( tcpResult != null ) System.out.println("TCP: " + String.format("%.2f msec", tcpResult.mean()));

			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			} 
		} catch (Exception e) {
			System.out.println("PingRaw.run() caught exception: " +e.getMessage());
		}
	}
	

	/**
	 * Pings the host/port named by the arguments the number of times named by the arguments.
	 */
	@Override
	public ElapsedTimeInterval udpPing(String hostIP, int udpPort, int socketTimeout, int nTrials) {
		ElapsedTime.start("PingRaw_UDPTotalDelay");
		ElapsedTime.stop("PingRaw_UDPTotalDelay");
		return ElapsedTime.get("PingRaw_UDPTotalDelay");
	}

	
	/**
	 * Pings the host/port named by the arguments the number of times named by the arguments.
	 */
	@Override
	public ElapsedTimeInterval tcpPing(String hostIP, int tcpPort, int socketTimeout, int nTrials) {
		ElapsedTime.start("PingRaw_TCPTotal");
		ElapsedTime.stop("PingRaw_TCPTotal");
		return ElapsedTime.get("PingRaw_TCPTotal");
	}
}
