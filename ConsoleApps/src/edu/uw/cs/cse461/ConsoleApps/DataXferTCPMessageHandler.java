package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONObject;

import edu.uw.cs.cse461.Net.Base.DataXferRawService;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.Net.TCPMessageHandler.DataXferTCPMessageHandlerService;
import edu.uw.cs.cse461.Net.TCPMessageHandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public class DataXferTCPMessageHandler extends NetLoadableConsoleApp implements DataXferInterface {
	private static final String TAG="DataXferTCPMessageHandler";
	
	// ConsoleApp's must have a constructor taking no arguments
	public DataXferTCPMessageHandler() throws Exception {
		super("dataxfertcpmessagehandler", true);
	}

	@Override
	public void run() throws Exception {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

			ConfigManager config = NetBase.theNetBase().config();
			String server = config.getProperty("dataxfertcpmessagehandler.server");
			if ( server == null ) {
				System.out.print("Enter a host ip, or exit to exit: ");
				server = console.readLine();
				if ( server == null ) return;
				if ( server.equals("exit")) return;
			}

			int port = config.getAsInt("dataxfertcpmessagehandler.port", -1, TAG);
			if ( port == -1 ) {
				System.out.print("Enter port number, or empty line to exit: ");
				String portStr = console.readLine();
				if ( portStr == null || portStr.trim().isEmpty() ) return;
				port = Integer.parseInt(portStr);
			}
			
			int socketTimeout = config.getAsInt("dataxfertcpmessagehandler.sockettimeout", -1, TAG);
			if ( socketTimeout < 0 ) {
				System.out.print("Enter socket timeout (in msec.): ");
				String timeoutStr = console.readLine();
				socketTimeout = Integer.parseInt(timeoutStr);
				
			}

			int nTrials = config.getAsInt("dataxfertcpmessagehandler.ntrials", -1, TAG);
			if ( nTrials == -1 ) {
				System.out.print("Enter number of trials: ");
				String trialStr = console.readLine();
				nTrials = Integer.parseInt(trialStr);
			}

			int xferLength = config.getAsInt("dataxfertcpmessagehandler.transfersize", -1, TAG);
			if ( xferLength == -1 ) {
				System.out.print("Enter transfer size (-1 to exit): ");
				String xferStr = console.readLine();
				xferLength = Integer.parseInt(xferStr);
				if(xferLength == -1)
					return;
			}
			
			

			TransferRate.clear();

			System.out.println("\n" + xferLength + " bytes");

			//-----------------------------------------------------
			// TCP transfer
			//-----------------------------------------------------

			TransferRateInterval tcpStats = DataXfer(server, port, socketTimeout, xferLength, nTrials);

			System.out.println("\nTCP: xfer rate = " + String.format("%9.0f", tcpStats.mean() * 1000.0) + " bytes/sec.");
			System.out.println("TCP: failure rate = " + String.format("%5.1f", tcpStats.failureRate()) +
					           " [" + tcpStats.nAborted()+ "/" + tcpStats.nTrials() + "]");

		} catch (Exception e) {
			System.err.println("Unanticipated exception: " + e.getMessage());
		}
	}
	
	public TransferRateInterval DataXfer(String hostIP, int port, int socketTimeout, int xferLength, int nTrials) throws Exception {
	
		for(int i = 0; i < nTrials; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			
			Socket socket = null;
			TransferRate.start("tcpMsgHandler"); //start the timer
			
			try {
				//create socket and connection
				socket = new Socket(hostIP, port);
				socket.setSoTimeout(socketTimeout);
				
				TCPMessageHandler tcpMH = new TCPMessageHandler(socket);
				tcpMH.setMaxReadLength(DataXferTCPMessageHandlerService.MAX_SINGLE_TRANSFER_SIZE);
				
				JSONObject transferSizeObject = new JSONObject();
				transferSizeObject.put(TCPMessageHandler.TRANSFER_SIZE_KEY, xferLength);
				tcpMH.sendMessage(transferSizeObject);
				
				int msgsExpected = (int) Math.ceil(1.0 * xferLength / DataXferTCPMessageHandlerService.MAX_SINGLE_TRANSFER_SIZE);
				int numBytesRecieved = 0;
				for(int msg = 0; msg < msgsExpected; msg++) {
					numBytesRecieved += tcpMH.readMessageAsBytes().length;
				}
				
				if(numBytesRecieved == xferLength) {
					TransferRate.stop("tcpMsgHandler", numBytesRecieved);
				} else {
					TransferRate.abort("tcpMsgHandler", numBytesRecieved);
					System.err.println("TCP Socket did not recieve all data: " + numBytesRecieved + " of " + xferLength + " bytes recieved");
				}
			} catch (UnknownHostException e) { //If you can't find the host, abort the run
	            TransferRate.abort("tcpMsgHandler", 0);
	            System.err.println("Could not find host: " + hostIP);
	        } catch (IOException e) { //If you cannot establish the IO stream, abort the run
	            TransferRate.abort("tcpMsgHandler", 0);
	            System.err.println("Could not get I/O for the connection to: " + hostIP);
	            e.printStackTrace();
	        } finally {
	        	//close the socket
				if(socket != null) {
					try {
						socket.close();
					} catch(IOException e) {
						//Should never happen
					}
				}
	        }
			
		}
		return TransferRate.get("tcpMsgHandler");
	}
}
