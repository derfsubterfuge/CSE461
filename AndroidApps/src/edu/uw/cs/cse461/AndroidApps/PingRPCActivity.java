package edu.uw.cs.cse461.AndroidApps;

import java.net.Socket;

import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.RPC.RPCCall;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.Net.TCPMessageHandler.TCPMessageHandler;
import edu.uw.cs.cse461.util.BackgroundToast;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.ContextManager;
import edu.uw.cs.cse461.util.IPFinder;
import edu.uw.cs.cse461.util.SampledStatistic.ElapsedTime;


public class PingRPCActivity extends NetLoadableAndroidApp {
	private static final String TAG="PingRPCActivity";
    public static final String PREFS_NAME = "CSE461";
	
	private String mMyIP;
	private String mServerIP;
	private String mServerPort;
	
	/**
	 * A public constructor with no arguments is required to function correctly
	 * as a NetLoadableAndroidApp. 
	 */
	public PingRPCActivity() {
		super("PingRPC", true);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        // select the screen to be displayed
        setContentView(R.layout.pingtcpmessagehandler_layout);
        
        // save my context so that this app can retrieve it later (?)
        ContextManager.setActivityContext(this);
    }
    
    /**
     * Called after we've been unloaded from memory and are restarting.  E.g.,
     * 1st launch after power-up; relaunch after going Home.
     */
    @Override
    protected void onStart() {
    	super.onStart();
        Log.d(TAG, "onStart");
        
		ConfigManager config = NetBase.theNetBase().config();

		// Set initial values for the server name and port text boxes
		// See if there are values in the config file...
		String defaultServer = config.getProperty("echorpc.server", "cse461.cs.washington.edu");
		String defaultPort = config.getProperty("echorpc.port", "46120");

		// If we saved values the user provided during the last run of this program, use those
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        mServerIP = settings.getString("serverip", defaultServer );
        mServerPort = settings.getString("serverport", defaultPort);

        // Now set the text in the UI elements
    	TextView ipBox = ((TextView)findViewById(R.id.pingtcpmessagehandler_iptext));
    	if ( ipBox != null ) ipBox.setText(mServerIP);
    	
    	// LOCATE THE NEW UI COMPONENT YOU ADDED AND SET ITS INITIAL VALUE
    		//TextView portBox = ((TextView)findViewById(R.id.pingtcpmessagehandler_porttext));
    		//if ( portBox != null ) portBox.setText(mServerPort);    	
        
    	
    	// Now determine our IP address and display it.
    	
    	// (You may) Have to use a background thread in Android 4.0 and beyond -- can't
    	// touch network with main thread.
        new Thread(){
        	public void run() {
                try {
                	mMyIP = IPFinder.getMyIP();
                	
                	if ( mMyIP != null ) runOnUiThread(new Runnable() {
                		                      public void run() {
                	    	                        TextView myIpText = (TextView)findViewById(R.id.pingtcpmessagehandler_myiptext);
                	    	                        if ( myIpText != null ) myIpText.setText(mMyIP);
                		                      }
                		                 });
                } catch (Exception e) {
                	Log.e(TAG, "Caught exception trying to display ip/port");
                }
        	}
        }.start();
    }

    /**
     * Called, for example, if the user hits the Home button.
     */
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(TAG, "onStop");
    	
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("serverip", mServerIP);
    	editor.putString("serverport", mServerPort);
    	editor.commit();
    }
    
    /**
     * System is shutting down...
     */
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG,"onDestroy");
    }
    
    public void onGoClicked(View b) {
    	String msg = "reached onGoClicked()"; 
    	Log.d(TAG, msg);
    	_setOutputText(msg);

    	// Starting Android 4.0, can't use the main thread to touch the network.
    	// So, most of your code goes inside this thread's run() method.
    	new Thread(){
    		public void run() {
    			try {
    				runOnUiThread(new Runnable() {
    					public void run() {
    						try {
						    	Log.d(TAG, "Reading user inputs...");
						    	// get server ip, port, socket timeout
								readUserInputs(); 
								ConfigManager config = NetBase.theNetBase().config();
								int socketTimeout = config.getAsInt("ping.sockettimeout", 500, TAG);
						    	Log.d(TAG, "Successfully read user inputs. ServerIP = " + mServerIP + ", Port = " + mServerPort);
								
								ElapsedTime.start("PingRPC");
								JSONObject response = RPCCall.invoke(mServerIP, Integer.parseInt(mServerPort), "echorpc", "echo", new JSONObject());
								ElapsedTime.stop("PingRPC");
								
	    						_setOutputText("Ran a ping test on server with IP:" + mServerIP + " on port: " + mServerPort + 
	    								" Total time = " + String.format("%.2f msec", ElapsedTime.get("PingRPC").mean()));

							} catch (Exception e) {
								ElapsedTime.abort("PingRPC");
			    				Log.e(TAG, "Caught exception: " + e.toString() + e.getMessage());
			    				BackgroundToast.showToast(ContextManager.getActivityContext(), "Ping attempt failed: " + e.getMessage(), Toast.LENGTH_LONG);
							}
    					}
    				});
    			} catch (Exception e) {
    				Log.e(TAG, "Caught exception: " + e.getMessage());
    				BackgroundToast.showToast(ContextManager.getActivityContext(), "Ping attempt failed: " + e.getMessage(), Toast.LENGTH_LONG);
    			}
    		}
    	}.start();
    }
    
    /**
     * Retrieve text from screen widgets.
     * @throws Exception
     */
    private void readUserInputs() throws Exception {
    	mServerIP = ((TextView)findViewById(R.id.pingtcpmessagehandler_iptext)).getText().toString().trim();
    	mServerPort = ((TextView)findViewById(R.id.pingtcpmessagehandler_porttext)).getText().toString().trim();
    }

    private void _setOutputText(String msg) {
    	TextView outputText = (TextView)findViewById(R.id.pingtcpmessagehandler_outputtext);
		if ( outputText != null ) outputText.setText(msg);
    }
}