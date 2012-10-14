package edu.uw.cs.cse461.AndroidApps;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.util.BackgroundToast;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.ContextManager;

/**
 * A simple Android echo client.  It uses UDP for communiation.  Messages must fit in a single packet.
 * @author zahorjan
 *
 */
public class EchoRawActivity extends NetLoadableAndroidApp {
	private static final String TAG="EchoRawActivity";
    public static final String PREFS_NAME = "CSE461ECHORAW";
	
	private String mMyIP;
	private String mServerIP;
	private String mServerPort;
	private String mMsg;
	
	private String mResult;
	
	private int mSocketTimeout;
	
	/**
	 * The infrastructure requires a parameterless, public constructor for all NetLoadableAndroidApp's.
	 */
	public EchoRawActivity() {
		/**
		 * The superclass constructor requires two arguments.  The first is the application name.
		 * The second indicates whether or not the application should be listed as available by the shell program.
		 */
		super("EchoRaw", true);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.echoraw_layout);
        
        // save my context so that this app can retrieve it later
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
        
        // the ConfigManager object represents the config.ini file
		ConfigManager config = NetBase.theNetBase().config();
		
		mSocketTimeout = config.getAsInt("echo.sockettimeout", 500, TAG);
		
		String defaultServer = config.getProperty("echoraw.server");
		if ( defaultServer == null ) defaultServer = "cse461.cs.washington.edu";
		String defaultPort = config.getProperty("echoraw.udpport");
		if ( defaultPort == null ) defaultPort = "46120";
		String defaultMsg = "echo me";

		// Are there saved values from a previous invocation of this app?
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        mServerIP = settings.getString("serverip", defaultServer );
        mServerPort = settings.getString("serverport", defaultPort);
        mMsg= settings.getString("message", defaultMsg);

    	((TextView)findViewById(R.id.echoraw_iptext)).setText(mServerIP);
    	((TextView)findViewById(R.id.echoraw_porttext)).setText(mServerPort);
    	((TextView)findViewById(R.id.echoraw_msgtext)).setText(mMsg);
    	   
    	// Trying to display our own IP.
    	// (May) have to use a background thread in Android 4.0 and beyond -- can't
    	// touch network with main thread.
        new Thread(){
        	public void run() {
                try {
                	mMyIP = ((RPCService)NetBase.theNetBase().getService("rpc")).localIP();
                	
                	runOnUiThread(new Runnable() {
                		public void run() {
                        	String msg = mMyIP + ":" + Integer.toString(((RPCService)NetBase.theNetBase().getService("rpc")).localPort());
                	    	TextView myIpText = (TextView)findViewById(R.id.echoraw_myiptext);
                        	if ( myIpText != null ) myIpText.setText(msg);
                		}
                	});
                	
                } catch (Exception e) {
                	Log.e(TAG, "Caught exception trying to display ip/port");
                }
        	}
        }.start();
    }

    /**
     * Called whenever Anroid infrastructure feels like it; for example, if the user hits the Home button.
     */
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(TAG, "onStop");
    	
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("serverip", mServerIP);
    	editor.putString("serverport", mServerPort);
    	editor.putString("message", mMsg);
    	editor.commit();
    }
    
    /**
     * Retrieve text from screen widgets.
     * @throws Exception
     */
    private void readUserInputs() throws Exception {
    	mServerIP = ((TextView)findViewById(R.id.echoraw_iptext)).getText().toString();
    	mServerPort = ((TextView)findViewById(R.id.echoraw_porttext)).getText().toString();
    	mMsg = ((TextView)findViewById(R.id.echoraw_msgtext)).getText().toString();
    }
    
    /**
     * When the echo button is clicked, we need to send the message to the server and read the reply.
     * @param b
     */
    public void onGoClicked(View b) {
    	try {
    		readUserInputs();
    		_setOutputText(""); // clear message area
    	} catch (Exception e) {
    		_setOutputText(e.getMessage());
    	}
    	new Thread(){
    		public void run() {
    			try {
    				DatagramSocket socket = new DatagramSocket();
    				socket.setSoTimeout(mSocketTimeout);
    				
    				byte[] buf = mMsg.getBytes();
    				DatagramPacket packet = new DatagramPacket( buf, buf.length, new InetSocketAddress(mServerIP, Integer.parseInt(mServerPort)) );
    				socket.send(packet);

    				byte[] receiveBuf = new byte[buf.length];
    				DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
    				socket.receive(receivePacket);  // may time out 
    				mResult = new String(receiveBuf, 0, receivePacket.getLength());
    				
    				runOnUiThread(new Runnable() {
    					public void run() {
    						String msg = mServerIP + ": " + mServerPort + "\n" + mResult;
    						_setOutputText(msg);
    					}
    				});
    			} catch (SocketTimeoutException e) {
    				String msg = "Server failed to respond within " + mSocketTimeout + " msec.";
    				Log.e(TAG, msg );
    				BackgroundToast.showToast(ContextManager.getActivityContext(), msg, Toast.LENGTH_LONG);
    			} catch (Exception e) {
    				String msg = "Echo attempt caught exception (" + e.getClass().getName() + "): " + e.getMessage();
    				Log.e(TAG, msg );
    				BackgroundToast.showToast(ContextManager.getActivityContext(), msg, Toast.LENGTH_LONG);
    			}
    		}
    	}.start();
    }

    private void _setOutputText(String msg) {
    	TextView outputText = (TextView)findViewById(R.id.echoraw_outputtext);
		outputText.setText(msg);
    }
}