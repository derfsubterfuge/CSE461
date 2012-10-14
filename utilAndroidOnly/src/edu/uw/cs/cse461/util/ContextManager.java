package edu.uw.cs.cse461.util;

import android.app.Activity;

public class ContextManager {
	private static Activity mActivityContext;
	
	public static void setActivityContext(Activity context) {
		mActivityContext = context;
	}
	public static Activity getActivityContext() {
		return mActivityContext;
	}
}
