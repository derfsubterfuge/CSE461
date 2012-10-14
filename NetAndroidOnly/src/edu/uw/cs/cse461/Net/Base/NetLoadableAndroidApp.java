package edu.uw.cs.cse461.Net.Base;

import android.app.Activity;
import edu.uw.cs.cse461.Net.Base.NetLoadableInterface.NetLoadableAndroidAppInterface;


/**
 * Interface for applications that run under Android.  Android apps are started by
 * our infrastructure using via Android intents, and so this class has no special methods.
 * Loadable Android apps live in the AndroidApps package.
 * 
 * @author zahorjan
 *
 */
public class NetLoadableAndroidApp extends Activity implements NetLoadableAndroidAppInterface {
	
	private String mLoadableName;    // set via argument provided to NetLoadable constructor
	private boolean mIsImplemented;  // also set via constructor argument
	
	
	protected NetLoadableAndroidApp(String name, boolean implemented) {
		mLoadableName = name;
		mIsImplemented = implemented;
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
	public String loadablename() { return mLoadableName; }
	
}
