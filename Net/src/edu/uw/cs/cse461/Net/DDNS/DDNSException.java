package edu.uw.cs.cse461.Net.DDNS;

import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The interfaces to many DDNS methods indicate that they may throw a DDNS exception,
 * so we need at least a minimal implementation of the class.  Here's a minimal implementation.
 * (The reference solution actually has much more than a minimal implementation, but what
 * to do is up to you.)
 * @author zahorjan
 *
 */
public class DDNSException extends Exception {
	
	public DDNSErrorCode errorCode;
	protected List<String> args;
	
	// My code has six subclasses of DDNSException, so I left them in 
	// this skeleton code.
	public static enum DDNSErrorCode {
		NOSUCHNAME(1, "NoSuchName"),       // name doesn't exist
		NOADDRESS(2, "NoAddress"),         // name exists, but has no registered address
		AUTHORIZATION(3, "Authorization"), // some operation required authorization caller didn't have
		RUNTIME(4, "Runtime"),             // misc. exception
		TTLEXPIRED(5, "TTLExpired"),       // exceeded max steps allowed
		ZONE(6, "Zone");                   // request about name not in zone
		
		public final String mString;
		public final int mInt;
		
		DDNSErrorCode(int i, String s) { mInt = i; mString = s; }
		@Override
		public String toString() { return mString; }
		public int toInt() { return mInt; }
	};
	
	/**
	 * Base class.  Only more specific subclass objects can be constructed.
	 * @param msg
	 */
	protected DDNSException(String msg) {
		super(msg);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}
	
	/**
	 * Catch-all exception for something went wrong.  The message should explain what.
	 * @author zahorjan
	 *
	 */
	public static class DDNSRuntimeException extends DDNSException {
		DDNSRuntimeException(String msg) {
			super(msg);
			errorCode = DDNSErrorCode.RUNTIME;
		}
	}
	
	/**
	 * A password was required, but either wasn't supplied or wasn't correct.
	 * @author zahorjan
	 *
	 */
	public static class DDNSAuthorizationException extends DDNSException {
		DDNSAuthorizationException(DDNSFullNameInterface name) {
			super("Bad password for '" + name + "'");
			errorCode = DDNSErrorCode.AUTHORIZATION;
			args = Arrays.asList(name.toString());
		}
	}
	
	/**
	 * The name doesn't exist - there is no DDNS entry for it.
	 * @author zahorjan
	 *
	 */
	public static class DDNSNoSuchNameException extends DDNSException {
		DDNSNoSuchNameException(DDNSFullNameInterface name) {
			super("No such name: '" + name + "'");
			errorCode = DDNSErrorCode.NOSUCHNAME;
			args = Arrays.asList(name.toString());
		}
	}

	/**
	 * The name exists and its record can hold an address, but it doesn't have an address.
	 * @author zahorjan
	 *
	 */
	public static class DDNSNoAddressException extends DDNSException {
		DDNSNoAddressException(DDNSFullNameInterface name) {
			super("No address exists for name '"+ name + "'");
			errorCode = DDNSErrorCode.NOADDRESS;
			args = Arrays.asList(name.toString());
		}
	}

	/**
	 * The maximum number of steps allowed to resolve the name has been exceed.
	 * @author zahorjan
	 *
	 */
	public static class DDNSTTLExpiredException extends DDNSException {
		DDNSTTLExpiredException(DDNSFullNameInterface name) {
			super("TTL expired resolving '" + name + "'");
			errorCode = DDNSErrorCode.TTLEXPIRED;
			args = Arrays.asList(name.toString());
		}
	}

	public static class DDNSZoneException extends DDNSException {
		DDNSZoneException(DDNSFullNameInterface name, DDNSFullNameInterface zone) {
			super("Name '" + name +"' isn't in my zone [" + zone + "]");
			args = Arrays.asList(name.toString(), zone.toString());
		}
	}

}
