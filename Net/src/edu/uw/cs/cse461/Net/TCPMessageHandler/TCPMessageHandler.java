package edu.uw.cs.cse461.Net.TCPMessageHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Sends/receives a message over an established TCP connection.
 * To be a message means the unit of write/read is demarcated in some way.
 * In this implementation, that's done by prefixing the data with a 4-byte
 * length field.
 * <p>
 * Design note: TCPMessageHandler cannot usefully subclass Socket, but rather must
 * wrap an existing Socket, because servers must use ServerSocket.accept(), which
 * returns a Socket that must then be turned into a TCPMessageHandler.
 *  
 * @author zahorjan
 *
 */
public class TCPMessageHandler implements TCPMessageHandlerInterface {
	private static final String TAG="TCPMessageHandler";
	
	public static final String TRANSFER_SIZE_KEY = "transferSize";
	public static final String ID_KEY = "id";
	public static final String HOST_KEY = "host";
	public static final String MSG_TYPE_KEY = "type";
	public static final String ACTION_KEY = "action";
	public static final String MSG_KEY = "msg";
	public static final String ADDITIONAL_OPTIONS_KEY = "options";
	
	private static final int LENGTH_PREFIX_SIZE = 4; //bytes
	
	private Socket socket = null;
	private InputStream input = null;
	private OutputStream output = null;
	private int maxReadLength = Integer.MAX_VALUE;
	
	//--------------------------------------------------------------------------------------
	// helper routines
	//--------------------------------------------------------------------------------------

	/**
	 * We need an "on the wire" format for a binary integer.
	 * This method encodes into that format, which is little endian
	 * (low order bits of int are in element [0] of byte array, etc.).
	 * @param i
	 * @return A byte[4] encoding the integer argument.
	 */
	protected static byte[] intToByte(int i) {
		ByteBuffer b = ByteBuffer.allocate(LENGTH_PREFIX_SIZE);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(i);
		byte buf[] = b.array();
		return buf;
	}
	
	/**
	 * We need an "on the wire" format for a binary integer.
	 * This method decodes from that format, which is little endian
	 * (low order bits of int are in element [0] of byte array, etc.).
	 * @param buf
	 * @return 
	 */
	protected static int byteToInt(byte buf[]) {
		ByteBuffer b = ByteBuffer.wrap(buf);
		b.order(ByteOrder.LITTLE_ENDIAN);
		return b.getInt();
	}

	/**
	 * Constructor, associating this TCPMessageHandler with a connected socket.
	 * @param sock
	 * @throws IOException
	 */
	public TCPMessageHandler(Socket sock) throws IOException {
		if(sock == null)
			throw new IllegalArgumentException("Socket cannot be null");
		socket = sock;
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}
	
	/**
	 * Closes resources allocated by this TCPMessageHandler; doesn't close the socket it's attached to.
	 * The TCPMessageHandler object is unusable after execution of this method.
	 */
	public void discard() {
		try {
			if(input != null)
				input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if(output != null)
				output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the maximum allowed size for which decoding of a message will be attempted.
	 * @return The previous setting of the maximum allowed message length.
	 */
	@Override
	public int setMaxReadLength(int maxLen) {
		if(maxLen < 0)
			throw new IllegalArgumentException("length cannot be less than 0");
		int oldLen = maxReadLength;
		maxReadLength = maxLen;
		return oldLen;
	}

	
	//--------------------------------------------------------------------------------------
	// send routines
	//--------------------------------------------------------------------------------------
	
	public void sendMessage(byte[] buf) throws IOException {		
			output.write(intToByte(buf.length));
			output.write(buf);
	}
	
	public void sendMessage(String str) throws IOException {
		sendMessage(str.getBytes());
	}
	
	public void sendMesssage(JSONArray jsArray) throws IOException {
		sendMessage(jsArray.toString());
	}
	
	public void sendMessage(JSONObject jsObject) throws IOException {
		sendMessage(jsObject.toString());
	}
	
	//--------------------------------------------------------------------------------------
	// read routines
	//--------------------------------------------------------------------------------------
	
	public byte[] readMessageAsBytes() throws IOException {
		
		int bytesRead = 0;
		int msgLengthBytesRead = 0;
		byte[] msgLenBytes = new byte[LENGTH_PREFIX_SIZE];
		while(msgLengthBytesRead < LENGTH_PREFIX_SIZE) {
			bytesRead = input.read(msgLenBytes, msgLengthBytesRead, LENGTH_PREFIX_SIZE - msgLengthBytesRead);
			if(bytesRead > 0) {
				msgLengthBytesRead += bytesRead;	
			} else if(bytesRead < 0) {
				throw new IOException("End of stream has already been reached.");
			}
		}
		
		int msgLen = byteToInt(msgLenBytes);
		if(msgLen < 0 || msgLen > maxReadLength) {
			throw new IOException("Recieved invalid message length: " + msgLen);
		}
		byte[] msgHolder = new byte[msgLen];
		int totalBytesRead = 0;
		while(totalBytesRead < msgLen) {
			bytesRead = input.read(msgHolder, totalBytesRead, msgLen - totalBytesRead);
			if(bytesRead > 0) {
				totalBytesRead += bytesRead;	
			} else if(bytesRead < 0) {
				throw new IOException("End of stream has already been reached.");
			}
		}	
		
		return Arrays.copyOf(msgHolder, totalBytesRead);
	}
	
	public String readMessageAsString() throws IOException {
		byte[] msgBytes = readMessageAsBytes();
		return new String(msgBytes);
	}
	
	public JSONArray readMessageAsJSONArray() throws IOException, JSONException {
		String msgString = readMessageAsString();
		return new JSONArray(msgString);
	}
	
	public JSONObject readMessageAsJSONObject() throws IOException, JSONException {
		String msgString = readMessageAsString();
		return new JSONObject(msgString);
	}

}
