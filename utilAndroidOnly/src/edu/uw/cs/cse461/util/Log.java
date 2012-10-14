package edu.uw.cs.cse461.util;

public class Log {
	static private int mLevel = 0;
	static private boolean mShowLog = true;
	
	/**
	 * This is a simple debug message class that implements
	 * filtering based on log level.  It's modeled after android.util.Log.
	 * <p>
	 * You choose a level at which to produce a message by calling, say,
	 * Log.d("some tag", "my message").  You can set the class to filter
	 * all messages below a client-specified level.
	 * 
	 * @author zahorjan
	 *
	 */
	public static enum DebugLevel {
		VERBOSE(android.util.Log.VERBOSE, "VERBOSE"), 
		DEBUG(android.util.Log.DEBUG, "DEBUG"), 
		INFO(android.util.Log.INFO, "INFO"), 
		WARN(android.util.Log.WARN, "WARN"), 
		ERROR(android.util.Log.ERROR, "ERROR"),
		ASSERT(android.util.Log.ASSERT, "ASSERT");
		private final int mInt;
		private final String mString;
		private DebugLevel(int level, String s) { mInt = level; mString = s;}
		public int toInt() { return mInt; }
		@Override
		public String toString() { return mString; }  
	};
	
	static private int _show(DebugLevel level, String tag, String msg) {
		if (  (mShowLog && level.toInt() >= mLevel) || level==DebugLevel.ASSERT) {
			return android.util.Log.println(level.toInt(), tag, msg);			
		}
		return 0;
	}
	
	static public int setLevel(int level) {
		int old = mLevel;
		mLevel = level;
		return old;
	}
	
	static public boolean setShowLog(boolean b) {
		boolean old = mShowLog;
		mShowLog = b;
		return old;
	}
	
	static public int v(String tag, String msg) { return _show(DebugLevel.VERBOSE, tag, msg); }
	static public int d(String tag, String msg) { return _show(DebugLevel.DEBUG, tag, msg); }
	static public int i(String tag, String msg) { return _show(DebugLevel.INFO, tag, msg); }
	static public int w(String tag, String msg) { return _show(DebugLevel.WARN, tag, msg); }
	static public int e(String tag, String msg) { return _show(DebugLevel.ERROR, tag, msg); }
	static public int wtf(String tag, String msg) { return _show(DebugLevel.ASSERT, tag, msg); }
}
