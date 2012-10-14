package edu.uw.cs.cse461.Net.DDNS;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.Net.DDNS.DDNSException.DDNSRuntimeException;



/**
 * Utility class representing a single entry in the ddns cache.
 * <p>
 * Fields:
 * <ul>
 * <li>tag - A string annotation (usually "host:type", but can be arbitrary)
 * <li>type - Primarily used to indicate "no record found," or else whether it was a host lookup or an app lookup
 * <li>ip - The resolved ip, or null if none found.  (Note: The "ip" can be an Internet DNS name.  Java allows both where it allows either.)
 * <li>port - The value field of the record.  When an app lookup is performed, it's the port.  When a host lookup is done, it's null.
 * </ul>
 * @author zahorjan
 *
 */
public class DDNSRRecord {		
	protected RRType mType;  

	public static enum RRType {
		RRTYPE_A(1, "A"),
		RRTYPE_NS(2, "NS"),
		RRTYPE_SOA(3, "SOA"),
		RRTYPE_CNAME(4, "CNAME");
		
		private final String mString;
		private final int mInt;
		
		RRType(int i, String s) {
			mInt = i;
			mString = s;
		}
		
		@Override
		public String toString() { return mString;}
		public int toInt() { return mInt; }
	};
	
	/**
	 * Demarshall routine.
	 * @param obj JSON description of record
	 * @return
	 * @throws Exception
	 */
	static DDNSRRecord unmarshall(JSONObject obj) throws DDNSRuntimeException {
		try {
			String type = obj.getString("type");
			if ( type.equals("A") ) return new ARecord(obj);
			if ( type.equals("NS") ) return new NSRecord(obj);
			if ( type.equals("SOA") ) return new SOARecord(obj);
			if ( type.equals("CNAME") ) return new CNAMERecord(obj);
			throw new DDNSRuntimeException("Unrecognized RR type [ " + type + "]");
		} catch (JSONException e) {
			throw new DDNSRuntimeException("Invalid JSONObject representation of a RRecord: " + obj.toString());
		}
	}
	
	protected DDNSRRecord() {
	}
	
	protected DDNSRRecord(JSONObject obj) throws JSONException {
	}
	
	protected JSONObject marshall() throws JSONException {
		return new JSONObject().put("type", type().toString());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		return _toString(sb).toString();
	}
	
	protected StringBuilder _toString(StringBuilder sb) {
		sb.append("Type: ").append(mType.toString());
		return sb;
	}
	
	public RRType type() { return mType; }
	
	//-------------------------------------------------------------------
	// A record
	//-------------------------------------------------------------------
	
	public static class ARecord extends DDNSRRecord {
		protected String mIP;
		protected int mPort;

		public ARecord() {
			mType = RRType.RRTYPE_A;
			mIP = null;
			mPort = -1;
		}
		
		public ARecord(String ip, int port) {
			mType = RRType.RRTYPE_A;
			mIP = ip;
			mPort = port;
		}
		
		protected ARecord(JSONObject obj) throws JSONException {
			super(obj);
			mType = RRType.RRTYPE_A;
			mIP = obj.getString("ip");
			mPort = obj.getInt("port");
		}
		
		/**
		 * Intended for use by DDNSService only.
		 * @param ip
		 * @param port
		 * @return this
		 */
		ARecord updateAddress(String ip, int port) {
			mIP = ip;
			mPort = port;
			return this;
		}
		
		@Override
		protected JSONObject marshall() throws JSONException {
			JSONObject result = super.marshall();
			result.put("ip", ip())
				  .put("port", port());
			return result;
		}
		
		@Override
		protected StringBuilder _toString(StringBuilder sb) {
			super._toString(sb);
			sb.append(" IP: ").append(mIP)
			  .append(" Port: ").append(mPort);
			return sb;
		}
		
		public String ip() { return mIP; }
		public int port() { return mPort; }
	}
	
	//-------------------------------------------------------------------
	// NS record
	//-------------------------------------------------------------------
	
	public static class NSRecord extends ARecord {

		public NSRecord() {
			super(null,-1);
			mType = RRType.RRTYPE_NS;
		}
		
		protected NSRecord(JSONObject obj) throws JSONException {
			super(obj);
			mType = RRType.RRTYPE_NS;
		}
		
		@Override
		protected JSONObject marshall() throws JSONException {
			return super.marshall();
		}
		
		@Override
		protected StringBuilder _toString(StringBuilder sb) {
			return super._toString(sb);
		}
	}

	//-------------------------------------------------------------------
	// CNAME record
	//-------------------------------------------------------------------
	
	public static class CNAMERecord extends DDNSRRecord {
		protected DDNSFullNameInterface mAlias;
		
		public CNAMERecord(String alias) {
			mType = RRType.RRTYPE_CNAME;
			mAlias= new DDNSFullName(alias);
		}
		
		public CNAMERecord(DDNSFullNameInterface alias) {
			mType = RRType.RRTYPE_CNAME;
			mAlias= alias;
		}
		
		protected CNAMERecord(JSONObject obj) throws JSONException {
			super(obj);
			mType = RRType.RRTYPE_CNAME;
			mAlias = new DDNSFullName(obj.getString("alias"));
		}
		
		public DDNSFullNameInterface alias() {  return mAlias; }
		
		@Override
		protected JSONObject marshall() throws JSONException {
			return super.marshall().put("alias", alias().toString());
		}
		
		@Override
		protected StringBuilder _toString(StringBuilder sb) {
			super._toString(sb).append(" Alias: ").append(alias());
			return sb;
		}
	}

	//-------------------------------------------------------------------
	// SOA record
	//-------------------------------------------------------------------
	
	public static class SOARecord extends ARecord {
		
		public SOARecord() {
			mType = RRType.RRTYPE_SOA;
		}
		
		public SOARecord(String ip, int port) {
			super(ip, port);
			mType = RRType.RRTYPE_SOA;
		}
		
		protected SOARecord(JSONObject obj) throws JSONException {
			super(obj);
			mType = RRType.RRTYPE_SOA;
		}
		
		@Override
		protected StringBuilder _toString(StringBuilder sb) {
			return super._toString(sb);
		}
	}
	
}
