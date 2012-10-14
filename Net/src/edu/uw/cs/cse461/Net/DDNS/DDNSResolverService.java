package edu.uw.cs.cse461.Net.DDNS;

import org.json.JSONException;

import edu.uw.cs.cse461.HTTP.HTTPProviderInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.DDNS.DDNSRRecord.ARecord;


public class DDNSResolverService extends NetLoadableService implements HTTPProviderInterface, DDNSResolverServiceInterface {
	private static String TAG="DDNSResolverService";
	
	/**
	 * Called to end execution.  Specifically, need to terminate any threads we've created.
	 */
	@Override
	public void shutdown() {
		super.shutdown();
	}
		
	/**
	 * Serves web pages.  The 0th element of uriArray is always null.
	 * The next element names this service ("ddnsresolver").  The optional third
	 * component is a name to be resolved before dumping the cache.
	 */
	@Override
	public String httpServe(String[] uriArray) {
		StringBuilder sb = new StringBuilder();
		sb.append("Host:  ").append(NetBase.theNetBase().hostname()).append("\n");
		if ( uriArray.length > 2 ) {
			sb.append("Resolving: ").append(uriArray[2]).append("\n");
			// third component
			
			//  resolve uriArray[2] to an address and append the address to the stringbuilder
			// ....
			sb.append("You haven't updated DDNSResolverServer.httpServer()");
		}
		// add any additional information you want here
		return sb.toString();
	}
	
	
	public DDNSResolverService() throws DDNSException {
		super("ddnsresolver", true);
	}

	/**
	 * Unregisters a name.  
	 * @param name
	 * @throws DDNSException
	 */
	@Override
	public void unregister(DDNSFullNameInterface name) throws DDNSException, JSONException {
	}
	
	/**
	 * Registers a name as being on this host (IP) at the given port.
	 * If the name already exists, update its address mapping.  If it doesn't exist, create it (as an ARecord).
	 * @param name
	 * @param ip
	 * @param port
	 * @throws DDNSException
	 */
	@Override
	public void register(DDNSFullNameInterface name, int port) throws DDNSException {
	}
	
	/**
	 * Resolves a name to an ARecord containing an address.  Throws an exception if no ARecord w/ address can be found.
	 * @param name
	 * @return The ARecord for the name, if one is found
	 * @throws DDNSException
	 */
	@Override
	public ARecord resolve(String nameStr) throws DDNSException, JSONException {
		return null;
	}
	
	
	@Override
	public String dumpState() {
		return "whatever you want";
	}

}
