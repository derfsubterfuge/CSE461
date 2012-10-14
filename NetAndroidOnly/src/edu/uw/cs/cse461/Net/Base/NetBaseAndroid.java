package edu.uw.cs.cse461.Net.Base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import edu.uw.cs.cse461.Net.Base.NetLoadableInterface.NetLoadableAndroidAppInterface;
import edu.uw.cs.cse461.util.ContextManager;
import edu.uw.cs.cse461.util.Log;

public class NetBaseAndroid extends NetBase {
	private static final String TAG = "NetBaseAndroid";
	
	/**
	 * Used to keep start of loaded apps.
	 */
	private static HashMap<String, Class<? extends Object>> mAppMap = new HashMap<String, Class<? extends Object>>();

	public NetBaseAndroid() {
	}
	
	public void loadApps() {
		String startingApp = null; // for debugging output in catch block

		String[] appClassList = config().getAsStringVec("android.apps");
		if ( appClassList == null ) return;
		
		for (String appClassname : appClassList) {
			try {
				Log.d(TAG, "Recording app " + appClassname);
				startingApp = appClassname;
				Class<? extends Object> appClass = (Class<? extends Object>) Class.forName(appClassname);
				
				// If not yet implemented, skip it
				NetLoadableAndroidApp appInstance = (NetLoadableAndroidApp)appClass.newInstance();
				if ( !appInstance.isImplemented() ) {
					Log.w(TAG, "Service " + appInstance.loadablename() + " isn't yet implemented.  Skipping.");
					continue;
				}
				
				mAppMap.put(appInstance.loadablename(), appClass);
				
			} catch (ClassNotFoundException nfe) {
				Log.e(TAG, "ClassNotFoundException for " + startingApp);
			} catch (Exception e) {
				Log.e(TAG,
						"Exception loading app " + startingApp + ": "
								+ e.getMessage());
			}
		}
	}
	

	/**
	 * Runs an app.
	 * 
	 * @param appname
	 *            The name returned by the app's appname() method
	 */
	@Override
	public void startApp(String appname) throws Exception {
		Class<? extends Object> appClass = mAppMap.get(appname);
		if (appClass == null) throw new RuntimeException("App doesn't exist: '" + appname + "'");
		Intent intent = new Intent(ContextManager.getActivityContext(), appClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ContextManager.getActivityContext().startActivity(intent);
	}
	
	/**
	 * Returns a sorted list of the names of the loaded applications.
	 */
	@Override
	public List<String> loadedAppNames() {
		List<String> appList = new ArrayList<String>(mAppMap.keySet());
		java.util.Collections.sort(appList);		
		return appList;
	}

	@Override
	public NetLoadableAndroidAppInterface getApp(String appname) {
		check("getApp(" + appname + ")");
		//TODO: need to understand whether we can instantiate a single instance at load time and then invoke it, repeatedly, via intents.  Or not...
		//return mAppMap.get(appname);
		return null;
	}
	
	

}
