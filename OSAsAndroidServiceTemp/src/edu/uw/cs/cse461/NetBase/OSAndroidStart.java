package edu.uw.cs.cse461.NetBase;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import edu.uw.cs.cse461.util.Log;

public class OSAndroidStart extends BroadcastReceiver {
	public static final String TAG="OSAndroidStart";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if ( intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED )) {
			Intent startOSServiceIntent = new Intent(context, edu.uw.cs.cse461.NetBase.OSAndroidService.class);
			ComponentName startedService = context.startService(startOSServiceIntent);
			
			Log.e(TAG, "startedService = " + startedService!=null?startedService.toString():"null");
			if ( NetBase.theNetBase() != null ) {
				Log.e(TAG, "NetBase.theNetBase() not null");
				Log.e(TAG, "OS hostname = '" + NetBase.theNetBase().hostname() + "'");
			}
			else Log.e(TAG, "NetBase.theNetBase() is null");
		}
		else if ( intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
			//TODO: trigger OS.shutdown
		}
	}

}
