package edu.uw.cs.cse461.ConsoleApps;

import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTimeInterval;

public interface PingInterface {
	
	public interface PingRawInterface {
		public ElapsedTimeInterval udpPing(String hostIP, int udpPort, int socketTimeout, int nTrials);
		public ElapsedTimeInterval tcpPing(String hostIP, int tcpPort, int socketTimeout, int nTrials);
	}
	
	public interface PingTCPMessageHandlerInterface {
		public ElapsedTimeInterval ping(String hostIP, int port, int socketTimeout, int nTrials) throws Exception;
	}

	public interface PingRPCInterface {
		public ElapsedTimeInterval ping(String hostIP, int port, int nTrials) throws Exception;
	}
	
	public interface PingDDNSInterface {
		public ElapsedTimeInterval ping(String hostName, int nTrials);
	}

}