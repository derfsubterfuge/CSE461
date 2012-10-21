package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import edu.uw.cs.cse461.ConsoleApps.PingInterface.PingTCPMessageHandlerInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.TCPMessageHandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

/**
 * Raw sockets version of ping client.
 * @author zahorjan
 *
 */

public class PingTCPMessageHandler extends NetLoadableConsoleApp implements PingTCPMessageHandlerInterface {
	private static final String TAG="PingRaw";
	
	// ConsoleApp's must have a constructor taking no arguments
	public PingTCPMessageHandler() {
		super("pingtcpmessagehandler", true);
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
				System.out.println("tcp port: " + targetTCPPort);
				System.out.println("trials: " + nTrials);
				
				ElapsedTimeInterval tcpResult = null;

				if ( targetTCPPort != 0 ) {
					tcpResult = ping(targetIP, targetTCPPort, socketTimeout, nTrials);
				}

				if ( tcpResult != null ) System.out.println("TCP: " + String.format("%.2f msec", tcpResult.mean()));

			} catch (Exception e) {
				System.out.println("Exception: " + e.getMessage());
			} 
		} catch (Exception e) {
			System.out.println("PingTCPMessageHandler.run() caught exception: " +e.getMessage());
		}
	}


	@Override
	public ElapsedTimeInterval ping(String hostIP, int port, int socketTimeout, int nTrials) throws Exception {
		for (int i = 0; i < nTrials; i++) {
			System.out.println("TCP Trial " + (i+1));
			boolean aborted = false;
			ElapsedTime.start("PingRaw_TCPTotal");

			// create socket and connect to host
			try {
				Socket socket = new Socket(hostIP, port);
				try {
					socket.setSoTimeout(socketTimeout);
					
					TCPMessageHandler msgHandler = new TCPMessageHandler(socket);
					msgHandler.setMaxReadLength(100);
					byte[] data = new byte[0];
					msgHandler.sendMessage(data);
					msgHandler.readMessageAsBytes();
					
					msgHandler.discard();					
				} catch (SocketException ex) {
					System.out.println("\tError setting timeout! Aborting trial.");
					ElapsedTime.abort("PingRaw_TCPTotal");
					aborted = true;
				} catch (IOException ex) {
					System.out.println("\tTimeout!");
					ElapsedTime.abort("PingRaw_TCPTotal");
					aborted = true;
				} finally {
					// close connection
					socket.close();
					if (!aborted) 
						ElapsedTime.stop("PingRaw_TCPTotal");
				}

			} catch (UnknownHostException e) {
				System.out.println("\tInvalid hostname!");
				ElapsedTime.abort("PingRaw_TCPTotal");
			} catch (IOException e) {
				System.out.println("\tConnection error!");
				ElapsedTime.abort("PingRaw_TCPTotal");
			} catch (IllegalArgumentException e) {
				System.out.println("\tIllegal port number!");
				ElapsedTime.abort("PingRaw_TCPTotal");
			}
		}
		return ElapsedTime.get("PingRaw_TCPTotal");
	}
}
