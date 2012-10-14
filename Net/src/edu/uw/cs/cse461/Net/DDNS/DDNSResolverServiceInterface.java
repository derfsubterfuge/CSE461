package edu.uw.cs.cse461.Net.DDNS;

import edu.uw.cs.cse461.Net.DDNS.DDNSRRecord.ARecord;


public interface DDNSResolverServiceInterface {
	public void register(DDNSFullNameInterface name, int port) throws Exception;
	public void unregister(DDNSFullNameInterface name) throws Exception;
	public ARecord resolve(String nameStr) throws Exception;
}
