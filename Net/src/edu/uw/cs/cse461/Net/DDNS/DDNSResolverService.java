package edu.uw.cs.cse461.Net.DDNS;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.HTTP.HTTPProviderInterface;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.DDNS.DDNSRRecord.ARecord;
import edu.uw.cs.cse461.Net.RPC.RPCCall;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.IPFinder;


public class DDNSResolverService extends NetLoadableService implements HTTPProviderInterface, DDNSResolverServiceInterface {
	private static String TAG="DDNSResolverService";
	
	private String myIP;					/* the IP of the machine currently running this resolver */
	private String myName;					/* name of the machine currently running this resolver */
	private String password;				/* the password to use when registering/unregistering names */
	private int maxResolveAttempts; 		/* the max number of rpc calls to make in attempt to resolve a name (to prevent loops) */
	
	private String rootServerIP;			/* the IP of the root server to contact when resolving names */
	private int rootPort;					/* the port of the root server to contact when resolving names */

	boolean hasCaching;						/* whether or not caching has been turned on */
	int cacheTimeout;						/* how long to wait before booting elements out of the cache */
	private Map<String, ARecord> cache;		/* a cache of resolved names -> IP/ports, an invalid/unregistered name maps to NULL */
	
	private Timer timer;					/* timer for scheduling events */

	public DDNSResolverService() throws DDNSException {
		super("ddnsresolver", true);
		
		// set "my" IP 
		myIP = IPFinder.getMyIP();
		
		// set up root server ip and port
		ConfigManager config = NetBase.theNetBase().config();
		rootServerIP = config.getProperty("ddns.rootserver");
		if (rootServerIP == null) {
			throw new DDNSException("No ddns.rootserver entry in config file.");
		}
		String rport = config.getProperty("ddns.rootport");
		if (rport == null) {
			throw new DDNSException("No ddns.rootport entry in config file.");
		}
		rootPort = Integer.parseInt(rport);
		
		// set up caching
		String cachettl = config.getProperty("ddns.cachettl");
		if (cachettl == null) {
			throw new DDNSException("No ddns.cachettl entry in config file.");
		}
		cacheTimeout = Integer.parseInt(cachettl);
		if (cacheTimeout > 0) {
			hasCaching = true;
		}
		cache = new HashMap<String, ARecord>();
		
		// setup password
		password = config.getProperty("ddnsresolver.password");
		if (password == null) {
			throw new DDNSException("No ddnsresolver.password entry in config file.");
		}
		
		// maximum # of RPC calls to make 
		String max = config.getProperty("ddnsresolver.serverttl");
		if (max == null) {
			throw new DDNSException("No ddnsresolver.serverttl entry in config file.");
		}
		maxResolveAttempts = Integer.parseInt(max);
		
		// for scheduling new tasks
		timer = new Timer();
		
		// finally - REGISTER OURSELVES
		int port = Integer.parseInt(config.getProperty("rpc.serverport"));
		myName = config.getProperty("net.hostname");
		register(new DDNSFullName(myName), port);
	}
	
	
	/**
	 * Called to end execution.  Specifically, need to terminate any threads we've created.
	 */
	@Override
	public void shutdown() {
		// stop any scheduled TIMER events
		timer.cancel();
		
		// UNregister ourselves
		try {
			unregister(new DDNSFullName(myName));
		} catch (DDNSException e) {
			// do nothing	
		} catch (JSONException e) {
			// do nothing
		}
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

	/**
	 * Unregisters a name.  
	 * @param name
	 * @throws DDNSException
	 */
	@Override
	public void unregister(DDNSFullNameInterface name) throws DDNSException, JSONException {			
		// create JSON object to send to RPC
		JSONObject unregisterObj = new JSONObject();
		try {
			unregisterObj.put("name", name.toString());
			unregisterObj.put("password", password);

			// send to RPC		
			JSONObject response = resolverHelper(name.toString(), "unregister", rootServerIP, rootPort, unregisterObj, 0);
			
			// if the response was an error, then process that
			// TODO: handle each error type differently?
			if (response.get("resulttype").equals("ddnsexception")) {
				// TODO: add retries, keep trying for 10 seconds, if still fails then throw exception		

				throw new DDNSException(response.getString("message"));
			} 
			// else everything is A-OK and we are done, let's update the cache to reflect that this name doesn't have address anymore
			cachePut(name.toString(), null);
		} catch (JSONException e) {
			throw new DDNSException("unregister encountered a JSON exception: " + e.getMessage());
		} catch (IOException e) {
			throw new DDNSException("unregister encountered an IO exception: " + e.getMessage());
		} 
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
		// create JSON object to send to RPC
		JSONObject registerObj = new JSONObject();
		try {
			registerObj.put("name", name.toString());
			registerObj.put("ip", myIP);
			registerObj.put("port", port);
			registerObj.put("password", password);
			
			// send to RPC		
			JSONObject response = resolverHelper(name.toString(), "register", rootServerIP, rootPort, registerObj, 0);
			
			// process response
			// TODO: handle each error type differently?
			if (response.get("resulttype").equals("ddnsexception")) {	// FAILURE
				// TODO: add retries, keep trying for 10 seconds, if still fails then throw exception		

				cachePut(name.toString(), null);						// record failure in cache
				throw new DDNSException(response.getString("message"));
			} else {													// SUCCESS
				int timeToLive = response.getInt("lifetime");
				timer.schedule(new RegisterTask(name, port), Math.max(90*timeToLive/100,500));
			}
		} catch (JSONException e) {
			throw new DDNSException("register encountered a JSON exception: " + e.getMessage());
		} catch (IOException e) {
			throw new DDNSException("register encountered an IO exception: " + e.getMessage());
		} 
	}
	
	/**
	 * Resolves a name to an ARecord containing an address.  Throws an exception if no ARecord w/ address can be found.
	 * @param name
	 * @return The ARecord for the name, if one is found
	 * @throws DDNSException
	 */
	@Override
	public ARecord resolve(String nameStr) throws DDNSException, JSONException {
		// check cache first
		if (hasCaching) {
			synchronized (cache) {
				if (cache.containsKey(nameStr)) {
					ARecord result = cache.get(nameStr);
					if (result == null) {
						throw new DDNSException("unable to resolve name: " + nameStr);
					}
					return result;
				}
			}
		}
		
		try {
			// create JSON object to send to RPC
			JSONObject resolveObj = new JSONObject();
			resolveObj.put("name", nameStr);
			JSONObject response = resolverHelper(nameStr, "resolve", rootServerIP, rootPort, resolveObj, 0);

			// TODO : handle each error differently?
			if (response.getString("resulttype").equals("ddnsexception")) {	// FAILURE
				cachePut(nameStr, null);									// record failure in cache
				throw new DDNSException(response.getString("message"));
			} else {  														// SUCCESS
				JSONObject node = response.getJSONObject("node");
				// either A or SOA
				ARecord result = (ARecord) DDNSRRecord.unmarshall(node);
				cachePut(nameStr, result);									// record success in cache
				return result;
			}
		} catch (IOException e) {
			throw new DDNSException("resolve encountered an IO exception: " + e.getMessage());
		}
	}
	
	/* Recursively attempts to call the requested RPC method until one of the following occurs:
	 * - the returned node has done: true
	 * - the returned node indicates an error occurred
	 * - an exception is thrown
	 * - more than maxResolveAttempts calls are made
	 * 
	 * Returns the JSONObject returned by the requested RPC method call. 
	 * (This object will either indicate an error, or give the ip/port of the requested name)
	 */
	private JSONObject resolverHelper(String name, String rpccall, String serviceIP, int servicePort, JSONObject obj, int attempts) 
																						throws DDNSException, JSONException, IOException {
		if (attempts >= maxResolveAttempts) {
			throw new DDNSException("Unable to " + rpccall + " requested name. Reached max # of attempts.");
		}
	
		JSONObject response = RPCCall.invoke(serviceIP, servicePort, "ddns", rpccall, obj);
		if (response.getString("resulttype").equals("ddnsexception") || response.getBoolean("done")) {
			return response;
		}
		// set up variables for next attempt
		String newIP;
		int newPort;
		JSONObject node = response.getJSONObject("node");
		String nodeType = node.getString("type");
		if (nodeType.equals("CNAME")) {
			String nameToReplace = node.getString("name");
			String alias = node.getString("alias");
			
			// TODO: verify this works!
			name = name.substring(0, name.indexOf(nameToReplace)) + alias;
			obj.put("name", name);
			newIP = rootServerIP;
			newPort = rootPort;
		} else {
			// NS record
			obj.put("name", node.getString("name"));
			newIP = node.getString("ip");
			newPort = node.getInt("port");
		}
		return resolverHelper(name, rpccall, newIP, newPort, obj, attempts+1);
	}

	
	@Override
	public String dumpState() {
		return "whatever you want";
	}
	
	
	// Adds the given name -> ip&port mapping to the cache 
	private void cachePut(String name, ARecord record) {
		if (hasCaching) {
			synchronized (cache) {
				cache.put(name, record);	// record unsuccessful attempt to resolve this name
			}
			timer.schedule(new CacheTask(name), cacheTimeout);
		}
	}
	
	
	// A timer task for scheduling cache cleaning events
	class CacheTask extends TimerTask {
		private String nameToRemove;
		
		public CacheTask(String name) {
			nameToRemove = name;
		}
		
		@Override
		public void run() {
			// remove item from cache
			synchronized (cache) {			
				if (cache.containsKey(nameToRemove)) {
					cache.remove(nameToRemove);
				}
			}
		}
	}
	
	// A timer task for scheduling re-registering events
	class RegisterTask extends TimerTask {
		private DDNSFullNameInterface name;
		private int port;
		
		
		public RegisterTask(DDNSFullNameInterface registername, int registerport) {
			name = registername;
			port = registerport;
		}
		
		@Override
		public void run() {
			try {
				register(name, port);
			} catch (DDNSException e) {
				// what to do here??
			}
		}
		
	}


}
