package edu.uw.cs.cse461.Net.Base;

/**
 * Base class for OS loadable classes.  In addition to the methods shown
 * here, each class implementing this interface must provide
 * a constructor that takes no arguments.
 *
 * @author zahorjan
 *
 */
public abstract class NetLoadable implements NetLoadableInterface {
	
	private String mLoadableName;    // set via argument provided to NetLoadable constructor
	private boolean mIsImplemented;  // also set via constructor argument
	
	/**
	 * Every NetLoadable subclass must have a public constructor taking no arguments.  It must
	 * invoke its immediate superclass's constructor, providing the loadable's name and whether or not
	 * it is implemented (both of which are known statically).  If isImplemented is false, the loadable
	 * will NOT be loaded.
	 */
	protected NetLoadable(String loadableName, boolean isImplemented) {
		mLoadableName = loadableName;
		mIsImplemented = isImplemented;
	}
	
	/**
	 * If isImplemented() returns false, the loadable is not actually loaded.
	 * This allows the distributed config file to include all loadables that will
	 * eventually be built, minimizing problems caused by forgetting to add a loadable
	 * to a config file.
	 * <p>
	 * You set the implementated flag for your code via an argument to your class's superclass constructor.
	 * 
	 * @return true if the service is implemented and ready for use by applications, false otherwise 
	 */
	public boolean isImplemented() { return mIsImplemented; }
	
	/**
	 * Returns the name used to identify the loadable.  Names must be unique,
	 * and may have global (cross-system) meaning.
	 * (For example, the instance for the RPC service should return "rpc".)
	 * @return
	 */
	public String loadablename() {
		return mLoadableName;
	}

	//---------------------------------------------------------------------------------------------------
	/**
	 * Interface for applications that run in console mode.
	 * Loadable console apps live in the ConsoleApps package.
	 * 
	 * @author zahorjan
	 *
	 */
	public static abstract class NetLoadableConsoleApp extends NetLoadable implements NetLoadableConsoleAppInterface {
		
		protected NetLoadableConsoleApp(String name, boolean implemented) {
			super(name, implemented);
		}
		
		/**
		 * This method is called each time the app is invoked via the AppManager. 
		 * @throws Exception
		 */
		public abstract void run() throws Exception;
	
		/**
		 * Called when the app should shut down.
		 */
		public void shutdown() {
		}
		
	}
	//---------------------------------------------------------------------------------------------------

	
	//---------------------------------------------------------------------------------------------------
	//
	// There is another class that is logically a subclass of NetLoadable -- the NetLoadableAnroidApp class.
	// Because of some issues involved with supporting both console and Android by a single code base,
	// the NetLoadableAndroidApp class is defined separated, in an Android project.
	//---------------------------------------------------------------------------------------------------

	
	//---------------------------------------------------------------------------------------------------
	/**
	 * Interface name defined to make it easy to identify loadable services.
	 * Loadable services have no generic "run" interface - logically, they start
	 * when they are loaded.  In practice, they may start threads when they are loaded
	 * (e.g., in their constructors), or loading them may simply make a set of methods available
	 * to other code (much like loading a library).
	 */
	public static abstract class NetLoadableService extends NetLoadable implements NetLoadableServiceInterface {
		protected NetLoadableService(String name, boolean implemented) {
			super(name, implemented);
		}
		
		/**
		 * Called when the service should shut down.  If the service has started 
		 * any threads, its important that they be terminated - otherwise, the application
		 * as a whole may not terminate.  You can do any other cleanup that's required 
		 * as well.
		 */
		public void shutdown() {
		}
		
		/**
		 * Produces a string describing the current state of the service.  Used for debugging.
		 * @return
		 */
		public abstract String dumpState();

	}
	//---------------------------------------------------------------------------------------------------
}
