package edu.uw.cs.cse461.AndroidApps;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.util.ContextManager;


public class VersionActivity extends NetLoadableAndroidApp {
	private static final String TAG="PingActivity";
    public static final String PREFS_NAME = "CSE461PING";
	
	private String mMyIP;
	
	public VersionActivity() {
		super("Version", true);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_version);
        
    }
    
    /**
     * Called after we've been unloaded from memory and are restarting.  E.g.,
     * 1st launch after power-up; relaunch after going Home.
     */
    @Override
    protected void onStart() {
    	super.onStart();
        Log.d(TAG, "onStart");
        
        // save my context so that this app can retrieve it later (?)
        ContextManager.setActivityContext(this);

    	// (May) have to use a background thread in Android 4.0 and beyond -- can't
    	// touch network with main thread.
        new Thread(){
        	public void run() {
                try {
                	mMyIP = ((RPCService)NetBase.theNetBase().getService("rpc")).localIP();
                	
                	runOnUiThread(new Runnable() {
                		public void run() {
                        	String msg = mMyIP + ":" + Integer.toString(((RPCService)NetBase.theNetBase().getService("rpc")).localPort());
                	    	TextView myIpText = (TextView)findViewById(R.id.version_myiptext);
                        	if ( myIpText != null ) myIpText.setText(msg);
                        	
                        	TextView versionText = (TextView)findViewById(R.id.version_versiontext);
                        	if ( versionText != null ) versionText.setText("Version " + NetBase.theNetBase().version());
                		}
                	});
                	
                } catch (Exception e) {
                	Log.e(TAG, "Caught exception trying to display ip/port");
                }
        	}
        }.start();
    }

    /**
     * System is shutting down...
     */
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG,"onDestroy");
    }
    
}