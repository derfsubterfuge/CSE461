package edu.uw.cs.cse461.ConsoleApps;

import edu.uw.cs.cse461.util.SampledStatistic.TransferRateInterval;

public interface DataXferInterface {
	
	public interface DataXferRawInterface {
		public TransferRateInterval udpDataXfer(String hostIP, int udpPort, int socketTimeout, int xferLength, int nTrials);
		public TransferRateInterval tcpDataXfer(String hostIP, int tcpPort, int socketTimeout, int xferLength, int nTrials);
	}
	
	public interface DataXferTCPMessageHandlerInterface {
		public TransferRateInterval DataXfer(String hostIP, int port, int socketTimeout, int xferLength, int nTrials) throws Exception;
	}

	public interface DataXferRPCInterface {
		public TransferRateInterval DataXfer(String hostIP, int port, int xferLength, int nTrials) throws Exception;
	}
	
	public interface DataXferDDNSInterface {
		public TransferRateInterval DataXfer(String hostName, int xferLength, int nTrials);
	}

}