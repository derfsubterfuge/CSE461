package edu.uw.cs.cse461.util;


/**
 * The public interface for the ConfigManager class, which provides a minimal wrapper extending
 * <code>java.util.Properties<code> (so public methods of that class may be used as well as the ones listed here).
 * The primary functions are to introduce int values (rather than just strings), to perform some validty checking
 * on those values, and to mask the exceptions thrown by <code>java.util.Properties</code> when you ask for the
 * value of a key that doesn't exist.  Those exceptions are replaced by logging a warning and returning a
 * caller-supplied default value.
 * <p>
 * <b>Note:</b> Like other interfaces in this project, this interface definition is provided
 * for documentation reasons - it's a succinct listing of the public methods.  It is not intended that
 * there ever be more than one implementation of this interface.
 * 
 * @author zahorjan
 *
 */
public interface ConfigManagerInterface {
	
	/**
	 * Converts the value associated with the key from a space separated list of strings
	 * to an array of String (e.g., "a.b c.d e f.g.h" to ["a.b", "c.d", "e", "f.g.h"]).
	 */
	public String[] getAsStringVec(String key);
	
	/**
	 * Return the integer value associated with the key argument.  Throws an exception if 
	 * the key doesn't exist in the config file.
	 */
	public int getAsInt(String key) throws NoSuchFieldException;
	/**
	 * Returns the integer value associated with the key argument.  Returns defaultVal if
	 * the key doesn't exist in the config file.   
	 */
	public int getAsInt(String key, int defaultVal, String TAG);
	/**
	 * Returns the integer value associated with the key argument.  Returns defaultVal if
	 * the key doesn't exist in the config file.   Adds a warning message to the log if 
	 * the returned value is less than minimum.
	 */
	public int getAsInt(String key, int defaultVal, int minimum, String TAG);
	/**
	 * Returns the integer value associated with the key argument.  Returns defaultVal if
	 * the key doesn't exist in the config file.   Adds a warning message to the log if 
	 * the returned value is less than minimum or greater than maximum.
	 */
	public int getAsInt(String key, int defaultVal, int minimum, int maximum, String TAG);

}
