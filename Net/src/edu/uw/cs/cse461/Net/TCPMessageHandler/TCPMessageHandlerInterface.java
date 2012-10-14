package edu.uw.cs.cse461.Net.TCPMessageHandler;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface TCPMessageHandlerInterface {
	
	/**
	 * Closes the underlying socket and associated streams.  The TCPMessageHandler object is 
	 * unusable after execution of this method.
	 */
	public void discard();
	
	//--------------------------------------------------------------------------------------
	// send routines
	//   What is actually sent is always byte[].  These routines must translate into
	//   byte[] for sending.
	//--------------------------------------------------------------------------------------
	
	public void sendMessage(byte[] buf) throws IOException;
	public void sendMessage(String str) throws IOException;
	public void sendMesssage(JSONArray jsArray) throws IOException;
	public void sendMessage(JSONObject jsObject) throws IOException;
	
	//--------------------------------------------------------------------------------------
	// read routines
	//   Data comes off the network as bytes.  The various read routines
	//   convert from bytes into the requested data type.  They're the
	//   inverses of the send routines.
	//--------------------------------------------------------------------------------------
	
	public byte[] readMessageAsBytes() throws IOException;
	public String readMessageAsString() throws IOException;
	public JSONArray readMessageAsJSONArray() throws IOException, JSONException;
	public JSONObject readMessageAsJSONObject() throws IOException, JSONException;
	
	public int setMaxReadLength(int maxLen);  // don't even try to read a messsage claiming to be longer than the arg value

}
