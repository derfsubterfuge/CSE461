package edu.uw.cs.cse461.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

public class IPFinder {
	private static final String TAG = "IPFinder";
	
	public static String getMyIP() {
		
		WifiManager wifiManager = null;
		Context context = ContextManager.getActivityContext();
		wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	int wifiIPInt = wifiInfo.getIpAddress();
    	String wifiIP = Formatter.formatIpAddress(wifiIPInt);
    	Log.e(TAG, "wifiIP = " + wifiIP);
    	return wifiIP;
	}
	
	/**
	 * How to reliably obtain the routable IP address of the local device
	 * under Android isn't exactly clear.  This method tries an approach
	 * that enumerates the device's network interfaces.  It doesn't successfully 
	 * discover a useful IP address, even when there is one.
	 */
	public static void logInterfaces() {
		try {
    	Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    	if ( interfaces == null ) {
    		Log.e(TAG, "No interfaces");
    		return;
    	}
    	NetworkInterface iface;
    	String addressString;
    	while ( interfaces.hasMoreElements() ) {
    		iface = interfaces.nextElement();
    		Log.e(TAG, "toString() = "  + iface.toString());
    		Log.e(TAG, "getName = " + iface.getName());
    		Log.e(TAG, "getDisplayName = " + iface.getDisplayName());
    		Log.e(TAG, "isLoopback = " + (iface.isLoopback()?"yes":"no"));
    		if ( iface.isLoopback() ) continue;
    		Log.e(TAG, "isUp = " + (iface.isUp()?"yes":"no"));
    		Enumeration<InetAddress>ipVec =  iface.getInetAddresses();
        	Log.e(TAG, "ipVec = ");
        	if ( ipVec.hasMoreElements() ) {
        		InetAddress address = ipVec.nextElement();
        		addressString = address.toString();
        		Log.e(TAG, "\ttoString() = " + addressString);
        		Log.e(TAG, "\tgetHostAddress() = " + address.getHostAddress());
        		//break;
        	}
    	}	
		} catch (Exception e) {
			Log.e(TAG, "Caught exception: " + e.getMessage());
		}
	}
}
