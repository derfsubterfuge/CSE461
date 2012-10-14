package edu.uw.cs.cse461.util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Android's Toast notifications can be created only by the main (UI) thread.
 * This class provides a convenience method for background threads that allows
 * Toast notifications to be displayed.
 * @author zahorjan
 *
 */
public class BackgroundToast implements Runnable {
	private Context mCtx;
	private String mMsg;
	private int mDuration;
	
	/**
	 * Background threads wanting to show a Toast notification should call this method.
	 * @param ctx
	 * @param msg
	 * @param duration
	 */
	public static void showToast(Context ctx, String msg, int duration) {
		((Activity)ctx).runOnUiThread( new BackgroundToast(ctx, msg, duration) );
	}
	
	private BackgroundToast(Context ctx, String msg, int duration) {
		mCtx = ctx;
		mMsg = msg;
		mDuration = duration;
	}
	
	public void run() {
		Toast.makeText(mCtx, mMsg, mDuration).show();
	}
	
}
