package edu.uw.cs.cse461.Net.Base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.Log;

/**
 * A singleton class (only one instance can exist) implementing core network stack functionality.
 * It is initialized by calling init() and then startServices().
 * It's shut down by calling shutdown(). Its major responsibility is to bring up
 * services, like rpc, and make them available to apps (via getService()).
 * <p>
 * Check NetBaseInterface.java for a more concise list of the interfaces available
 * to other code.
 * <p>
 * Note that some of the functionality of this class requires code specific to either console
 * or Android execution.  Thus, the concrete object of this class that is created (by either
 * ConsoleStart or AndroidStart) is of a subclass.  (See the NetAndroidOnly and NetConsoleOnly
 * projects.)
 * 
 * @author zahorjan
 * 
 */
public abstract class NetBase implements NetBaseInterface {
	private static final String TAG = "NetBase";

	private static Object mConstructorSyncObj = new Object();  // used to avoid race on constructing the singleton instance
	private static NetBase theNetBaseInstance;                 // the singleton NetBase instance (returned by theNetBase()) 
	
	private boolean mAmShutdown = true;
	private ConfigManager mConfig;
	private String mHostname;

	/**
	 * Used to keep track of started services. The String key is the name
	 * returned by the // service's servicename() method.
	 */
	private static HashMap<String, NetLoadableService> serviceMap = new HashMap<String, NetLoadableService>();

	/**
	 * Only this class can construct an instance of itself, and it does that only once, to initialize static variable theNetBaseInstance.
	 */
	protected NetBase() {
		synchronized(mConstructorSyncObj) {
			if ( theNetBaseInstance != null ) throw new RuntimeException("Trying to construct new NetBase object when one already exists"); 
			theNetBaseInstance = this;
		}
	}
	
	/**
	 * Returns the singleton NetBase instance
	 * @return
	 */
	public static NetBase theNetBase() {
		return theNetBaseInstance;
	}
	
	/**
	 * Returns a version number for this software.
	 */
	public String version() {
		return "1.0.3 9/26/12";
	}

	/**
	 * Brings up the NetBase, then loads services, then starts the shell program.
	 * 
	 * @param configMgr Configuration settings read by caller from a config file.
	 * @throws Exception
	 */
	@Override
	public synchronized void init(ConfigManager configMgr)
			throws Exception {
		
		// sanity check
		if (!mAmShutdown)
			throw new RuntimeException(
					"Call to NetBase.init() but NetBase is already initialized!");
		
		mConfig = configMgr;

		if (configMgr == null) Log.e(TAG, "configMgr is null in call to NetBase::init()");

		int showDebug = mConfig.getAsInt("debug.enable", 1, TAG);
		Log.setShowLog(showDebug != 0);
		int debugLevel = mConfig.getAsInt("debug.level", Log.DebugLevel.DEBUG.toInt(), TAG);
		Log.setLevel(debugLevel);

		// code uses the host name a lot, so cache it in it's typical form (no
		// trailing '.')
		mHostname = mConfig.getProperty("net.hostname");
		if (mHostname == null) {
			Log.w(TAG, "NetBase: no net.hostname entry in config file");
			mHostname = "";
		} else {
			// convert full name to more colloquial name (no trailing period) 
			if (mHostname.equals(".")) mHostname = "";
			else if (mHostname.endsWith("."))
				mHostname = mHostname.substring(0, mHostname.length() - 1);
		}
		Log.d(TAG, "Booting host '" + mHostname + "'");

		mAmShutdown = false; // at this point, we're up, but with no services
								// running

		// net services are loade whether running in console or android mode
		_startServices();

	}
	
	/**
	 * Starts the "network services" listed in the config file.
	 * The argument is an array of class names.
	 */
	private void _startServices() {
		String[] serviceClassList = config().getAsStringVec("net.services");
		if (serviceClassList == null) {
			Log.e(TAG, "Missing or empty net.services in config file.  The system won't run this way.");
			return;
		}

		String startingService = null; // for debugging output in catch block
		for (String serviceClassname : serviceClassList) {
			try {
				Log.d(TAG, "Starting service " + serviceClassname);
				startingService = serviceClassname;
				
				// Get the Java Class object
				Class<? extends Object> serviceClass = (Class<? extends Object>) Class.forName(serviceClassname);
				
				// Create an instance of the class
				NetLoadableService service = (NetLoadableService)serviceClass.newInstance();
				
				// If not yet implemented, skip it
				if ( !service.isImplemented() ) {
					Log.w(TAG, "Service " + service.loadablename() + " isn't yet implemented.  Skipping.");
					continue;
				}
				
				// Record the instance in a Map, keyed by the service's
				// self-proclaimed name
				serviceMap.put(service.loadablename(), service);
				Log.i(TAG, serviceClassname + " started");
				
			} catch (Exception e) {
				Log.e(TAG, "Error while starting service " + startingService
						+ ": " + e.getMessage());
			}
		}
	}

	@Override
	public List<String> loadedServiceNames() {
		List<String> serviceList = new ArrayList<String>(serviceMap.keySet());
		java.util.Collections.sort(serviceList);		
		return serviceList;
	}

	/**
	 * Returns true if NetBase.theNetBase() is ready for use; false otherwise.
	 */
	public boolean isUp() {
		return !mAmShutdown;
	}

	/**
	 * Shutdown associated services and the network. The main point is to terminate
	 * threads, so that app can terminate. If you don't shut down various
	 * services, the odds are the app won't terminate even if the main thread
	 * exits.
	 */
	public synchronized void shutdown() {
		if (mAmShutdown)
			return;
		Log.d(TAG, "NetBase shutting down...");
		try {
			for (String serviceName : serviceMap.keySet()) {
				NetLoadableService service = serviceMap.get(serviceName);
				service.shutdown();
			}
			// We can't remove items from the HashMap while iterating
			serviceMap.clear();
		} catch (Exception e) {
			Log.e(TAG, "Error shutting down services: " + e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		mAmShutdown = true;
	}

	/**
	 * Helper function that simply makes sure the NetBase is running when calls to it
	 * are made.
	 * 
	 * @param method
	 */
	protected void check(String method) {
		if (mAmShutdown)
			throw new RuntimeException("NetBase." + method
					+ " called when NetBase isn't in initialized state");
	}

	/**
	 * Get access to the configuration properties read from the config file
	 * specified at launch.
	 * 
	 * @return
	 */
	public ConfigManager config() {
		check("getConfig");
		return mConfig;
	}

	/**
	 * Returns this host's name, if it has one, otherwise null. (This isn't
	 * useful until Project 4.)
	 * 
	 * @return
	 */
	public String hostname() {
		check("hostname");
		return mHostname;
	}

	/**
	 * Takes the value returned by the loadablename() method of the service
	 * you're looking for, and returns that service. (E.g., call with arg "rpc"
	 * to get the RPC service.)
	 * 
	 * @param servicename
	 * @return
	 */
	@Override
	public NetLoadableService getService(String servicename) {
		check("getService(" + servicename + ")");
		return serviceMap.get(servicename);
	}

	/**
	 * Returns the IPv4 address of the host.  Tries to find one that is globally routable, but may not always succeed.
	 */
	@Override
	public String myIP() {
		return IPFinder.getMyIP();
	}

	/**
	 * Returns current Unix time (seconds since 1/1/1970).
	 * 
	 * @return
	 */
	@Override
	public long now() {
		return System.currentTimeMillis() / 1000L;
	}

	/**
	 * Loads applications, making them available for invocation.  The list of applications
	 * to load comes from the config file.
	 */
	public abstract void loadApps();
	
	/**
	 * Start an application previously loaded via loadApps call. 
	 */
	@Override
	public abstract void startApp(String appname) throws Exception;
	
	/**
	 * Fetch a previously loaded application.
	 */
	@Override
	public abstract NetLoadableInterface getApp(String appname);

	/**
	 * Return a sorted list of names of loaded applications.
	 */
	@Override
	public abstract List<String> loadedAppNames();
	
}
