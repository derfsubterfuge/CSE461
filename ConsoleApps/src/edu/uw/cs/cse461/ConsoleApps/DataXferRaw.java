package edu.uw.cs.cse461.ConsoleApps;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import edu.uw.cs.cse461.ConsoleApps.DataXferInterface.DataXferRawInterface;
import edu.uw.cs.cse461.Net.Base.DataXferRawService;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRate;
import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

/**
 * Raw sockets version of ping client.
 * @author zahorjan
 *
 */
public class DataXferRaw extends NetLoadableConsoleApp implements DataXferRawInterface {
	private static final String TAG="DataXferRaw";

	// ConsoleApp's must have a constructor taking no arguments
	public DataXferRaw() throws Exception {
		super("dataxferraw", true);
	}

	/**
	 * This method is invoked each time the infrastructure is asked to launch this application.
	 */
	@Override
	public void run() {
		
		try {

			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

			ConfigManager config = NetBase.theNetBase().config();
			String server = config.getProperty("dataxferraw.server");
			if ( server == null ) {
				System.out.print("Enter a host ip, or exit to exit: ");
				server = console.readLine();
				if ( server == null ) return;
				if ( server.equals("exit")) return;
			}

			int basePort = config.getAsInt("dataxferraw.baseport", -1, TAG);
			if ( basePort == -1 ) {
				System.out.print("Enter port number, or empty line to exit: ");
				String portStr = console.readLine();
				if ( portStr == null || portStr.trim().isEmpty() ) return;
				basePort = Integer.parseInt(portStr);
			}
			
			int socketTimeout = config.getAsInt("dataxferraw.sockettimeout", -1, TAG);
			if ( socketTimeout < 0 ) {
				System.out.print("Enter socket timeout (in msec.): ");
				String timeoutStr = console.readLine();
				socketTimeout = Integer.parseInt(timeoutStr);
				
			}

			int nTrials = config.getAsInt("dataxferraw.ntrials", -1, TAG);
			if ( nTrials == -1 ) {
				System.out.print("Enter number of trials: ");
				String trialStr = console.readLine();
				nTrials = Integer.parseInt(trialStr);
			}

			for ( int index=0; index<DataXferRawService.NPORTS; index++ ) {

				TransferRate.clear();
				
				int port = basePort + index;
				int xferLength = DataXferRawService.XFERSIZE[index];

				System.out.println("\n" + xferLength + " bytes");

				//-----------------------------------------------------
				// UDP transfer
				//-----------------------------------------------------

				TransferRateInterval udpStats = udpDataXfer(server, port, socketTimeout, xferLength, nTrials);
				
				System.out.println("UDP: xfer rate = " + String.format("%9.0f", udpStats.mean() * 1000.0) + " bytes/sec.");
				System.out.println("UDP: failure rate = " + String.format("%5.1f", udpStats.failureRate()) +
						           " [" + udpStats.nAborted() + "/" + udpStats.nTrials() + "]");

				//-----------------------------------------------------
				// TCP transfer
				//-----------------------------------------------------

				TransferRateInterval tcpStats = tcpDataXfer(server, port, socketTimeout, xferLength, nTrials);

				System.out.println("\nTCP: xfer rate = " + String.format("%9.0f", tcpStats.mean() * 1000.0) + " bytes/sec.");
				System.out.println("TCP: failure rate = " + String.format("%5.1f", tcpStats.failureRate()) +
						           " [" + tcpStats.nAborted()+ "/" + tcpStats.nTrials() + "]");

			}
			
		} catch (Exception e) {
			System.err.println("Unanticipated exception: " + e.getMessage());
		}
	}
	
	/**
	 * Performs nTrials trials via UDP of a data xfer to host hostIP on port udpPort.  Expects to get xferLength
	 * bytes in total from that host/port.  Is willing to wait up to socketTimeout msec. for new data to arrive.
	 * @return A TransferRateInterval object that measured the total bytes of data received over all trials and
	 * the total time taken.  The measured time should include socket creation time.
	 */
	@Override
	public TransferRateInterval udpDataXfer(String hostIP, int udpPort, int socketTimeout, int xferLength, int nTrials) {
		for(int i = 0; i < nTrials; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			
			int totalBytesRecieved = 0;
			try {
				TransferRate.start("udp");
				DatagramSocket socket;
				
					socket = new DatagramSocket();
				
				DatagramPacket packet = new DatagramPacket(new byte[1000], 1000, InetAddress.getByName(hostIP), udpPort);
				socket.setSoTimeout(socketTimeout);
				socket.send(packet);
				
				while(totalBytesRecieved < xferLength) {
					socket.receive(packet);
					totalBytesRecieved += packet.getLength();
				}
				
				socket.close();
				if(totalBytesRecieved == xferLength) {
					TransferRate.stop("udp", totalBytesRecieved);
				} else {
					TransferRate.abort("udp", totalBytesRecieved);
					System.err.println("UDP Socket did not recieve all data.");
				}
			} catch (SocketException e) {
				TransferRate.abort("udp", totalBytesRecieved);
				System.err.println("UDP Socket could not be created.");
			} catch (IOException e) {
				TransferRate.abort("udp", totalBytesRecieved);
				System.err.println("UDP Socket Timed Out.");
			}
		}
			
		return TransferRate.get("udp");
	}
	
	/**
	 * Performs nTrials trials via TCP of a data xfer to host hostIP on port tcpPort.  Expects to get xferLength
	 * bytes in total from that host/port.  Is willing to wait up to socketTimeout msec. for new data to arrive.
	 * @return A TransferRateInterval object that measured the total bytes of data received over all trials and
	 * the total time taken.  The measured time should include socket creation time.
	 */
	@Override
	public TransferRateInterval tcpDataXfer(String hostIP, int tcpPort, int socketTimeout, int xferLength, int nTrials) {
		byte[] bytesRecieved = new byte[xferLength];
		
		for(int i = 0; i < nTrials; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			
			Socket socket = null;
			InputStream is = null;
			TransferRate.start("tcp"); //start the timer
			
			try {
				//create socket and connection
				socket = new Socket(hostIP, tcpPort);
				socket.setSoTimeout(socketTimeout);
				is = socket.getInputStream();
			} catch (UnknownHostException e) { //If you can't find the host, abort the run
	            TransferRate.abort("tcp", 0);
	            System.err.println("Could not find host: " + hostIP);
	        } catch (IOException e) { //If you cannot establish the IO stream, abort the run
	            TransferRate.abort("tcp", 0);
	            System.err.println("Could not get I/O for the connection to: " + hostIP);
	        }
			
			int totalNumBytesRecieved = 0;
			if(socket != null && is != null) {
				try {
					//read the returned bytes
					int numBytesRecieved = 0;
					while(totalNumBytesRecieved < xferLength && numBytesRecieved >= 0) {
						numBytesRecieved = is.read(bytesRecieved);
						totalNumBytesRecieved += numBytesRecieved;
					}
					
					//if all bytes were recieved, stop the timer, otherwise abort it
					if(totalNumBytesRecieved == xferLength) {
						TransferRate.stop("tcp", totalNumBytesRecieved);
					} else {
						TransferRate.abort("tcp", totalNumBytesRecieved);
						System.err.println("TCP Socket did not recieve all data.");
					}
				} catch(IOException e) {
					TransferRate.abort("tcp", totalNumBytesRecieved);
					System.err.println("TCP Socket Timed Out.");
				}
			}
			
			//close the socket
			if(socket != null) {
				try {
					socket.close();
				} catch(IOException e) {
					//Should never happen
				}
			}
		}
		return TransferRate.get("tcp");
	}
	
}
