package edu.uw.cs.cse461.Net.Base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;
import edu.uw.cs.cse461.util.Log;


public class NetBaseConsole extends NetBase {
	private static final String TAG = "NetBaseConsole";
	
	/**
	 * Used to keep start of loaded apps.
	 */
	private static HashMap<String, NetLoadableConsoleApp> mAppMap = new HashMap<String, NetLoadableConsoleApp>();

	public NetBaseConsole() {
	}
	
	/**
	 * Instantiate and remember instances of the loadable services and apps that are (a) named in the config
	 * file, and (b) have set their isImplemented flag to true (meaning the code is in a state that
	 * loading it is a reasonable thing to try).
	 */
	@Override
	public void loadApps() {
		String startingApp = null; // for debugging output in catch block

		String[] appClassList = config().getAsStringVec("console.apps");
		if ( appClassList == null ) return;
		
		for (String appClassname : appClassList) {
			try {
				Log.d(TAG, "Starting app " + appClassname);
				startingApp = appClassname;
				Class<? extends Object> appClass = (Class<? extends Object>) Class.forName(appClassname);
				NetLoadableConsoleApp app = (NetLoadableConsoleApp)appClass.newInstance();
				
				// If not yet implemented, skip it
				if ( !app.isImplemented() ) {
					Log.w(TAG, "Service " + app.loadablename() + " isn't yet implemented.  Skipping.");
					continue;
				}
				
				mAppMap.put(app.loadablename(), app);
				Log.i(TAG, appClassname + " Loaded");
				
			} catch (ClassNotFoundException nfe) {
				Log.e(TAG, "ClassNotFoundException for " + startingApp);
			} catch (Exception e) {
				Log.e(TAG,
						e.getClass().getName() + " exception loading app " + startingApp + ": "
								+ e.getMessage());
			}
		}
	}
	

	/**
	 * Starts an execution of the app, which means calling the run() method
	 * of the single instance of the app.
	 * 
	 * @param appname
	 *            The name returned by the app's appname() method
	 */
	@Override
	public void startApp(String appname) throws Exception {
		NetLoadableConsoleApp app = mAppMap.get(appname);
		if (app == null)
			throw new RuntimeException("App doesn't exist: '" + appname + "'");
		app.run();
	}
	
	/**
	 * Returns the names of all loaded apps.
	 */
	@Override
	public List<String> loadedAppNames() {
		List<String> appList = new ArrayList<String>(mAppMap.keySet());
		java.util.Collections.sort(appList);		
		return appList;
	}

	/**
	 * Returns the single, instantiated instance.
	 */
	@Override
	public NetLoadableConsoleApp getApp(String appname) {
		check("getApp(" + appname + ")");
		return mAppMap.get(appname);
	}
	
	

}
