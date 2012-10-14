package edu.uw.cs.cse461.HTTP;

import java.io.IOException;
import java.util.Properties;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableService;
import edu.uw.cs.cse461.Net.DDNS.DDNSFullName;
import edu.uw.cs.cse461.Net.DDNS.DDNSResolverService;
import edu.uw.cs.cse461.util.Log;

/**
 * Implements a simple HTTP server.  The URI's are of form http://host:ip/<servicename>/....
 * It's up to the service to implement the HTTPProvider interface.
 * <p>
 * There's a small, technical issue that affects the implementation.  The service
 * loading code, in the OS, needs a constructor that takes no arguments.  We want to
 * extend NanoHTTPD, though, and its constructor requires a port number.  Thus, the 
 * public class contains an inner class that extends NanoHTTPD.
 * @author zahorjan
 *
 */
public class HTTPDService extends NetLoadableService {
	private static final String TAG="HTTPDService";
	
	private boolean mIsUp;

	private NanoHTTPDService mNanoService;
	private boolean mHaveRegistered = false;  // records whether or not this server's name has been registered with DDNS
	private String mWwwName;

	@Override
	public String dumpState() {
		StringBuilder sb = new StringBuilder();
		sb.append(loadablename()).append(" is ").append(mIsUp ? "up" : "down").append("\n");
		if (!mHaveRegistered) sb.append("Not registered with DDNS\n");
		else sb.append("Registered with DDNS as ").append(mWwwName).append("\n");
		return sb.toString();
	}
	
	@Override
	public void shutdown() { 
		mIsUp = false;
		if ( mNanoService != null ) mNanoService.stop();
		mNanoService = null;
		if ( mHaveRegistered ) try {
			DDNSResolverService resolver = (DDNSResolverService)NetBase.theNetBase().getService("ddnsresolver");
			if ( resolver == null ) Log.w(TAG, "No local resolver.  Can't unregister name www");
			else resolver.unregister(new DDNSFullName(NetBase.theNetBase().hostname() + ".www") );  
		} catch (Exception e) {
			Log.w(TAG, "ADVISORY: Caught exception while unregistering with parent:\n" + e.getMessage());
		}
		
		super.shutdown();
	}

	private class NanoHTTPDService extends NanoHTTPD {
		/**
		 * Specify port 0 if you don't care what port the name server uses.
		 * @param port
		 * @throws IOException
		 */
		public NanoHTTPDService(int port) throws IOException {
			super(port,null); // nano won't like the null webroot, if it ever uses it, preventing inadvertently allowing access to local files via nano 
		}

		@Override
		public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
			if ( uri == null ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);
			try {
				Log.i(TAG,  "method = '" + method + "'  uri=" + uri);

				String[] uriVec = uri.split("/");
				if ( uriVec.length < 1 ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);

				try {
					HTTPProviderInterface provider = (HTTPProviderInterface)NetBase.theNetBase().getService(uriVec[1]);
					if ( provider == null ) provider = (HTTPProviderInterface)NetBase.theNetBase().getApp(uriVec[1]);
					if ( provider == null ) return new Response( HTTP_NOTFOUND, MIME_HTML, HTTP_NOTFOUND);
					String response = provider.httpServe(uriVec);
					String mimeType = MIME_PLAINTEXT;
					if ( response.startsWith("<html>")) mimeType = MIME_HTML;
					return new Response( HTTP_OK, mimeType, response );
				} catch (Exception e) {
					Log.e(TAG, "server response exception:");
					e.printStackTrace();
					return new Response( HTTP_NOTFOUND, MIME_PLAINTEXT, e.getMessage());
				}

			} catch (Exception e) {
				Log.e(TAG, "server: " + e.getMessage());
				e.printStackTrace();
				return new Response( HTTP_INTERNALERROR, MIME_HTML, HTTP_INTERNALERROR + "<p><pre>" + e.getMessage() + "</pre>");
			}
		}
	}
	
	int httpdPort() {
		if ( mNanoService != null ) return mNanoService.localPort();
		return -1;
	}

	public HTTPDService() throws IOException {
		super("httpd", true);
		
		NetBase theNetBase = NetBase.theNetBase();
		int port = theNetBase.config().getAsInt("httpd.port", 0, 0, TAG);
		this.mNanoService = new NanoHTTPDService(port);
		port = httpdPort();
		mIsUp = true;

		// create a name entry for me, but dont' fail to start just because we can't register
		try {
			mHaveRegistered = false;
			mWwwName = theNetBase.config().getProperty("httpd.name");
			if ( mWwwName != null && !mWwwName.isEmpty()) {
				DDNSResolverService resolver = (DDNSResolverService)theNetBase.getService("ddnsresolver");
				if ( resolver == null ) Log.w(TAG, "No local resolver.  Can't register name " + mWwwName);
//				else resolver.register(new DDNSFullName(OS.hostname() + ".www"), port );
				else {
					resolver.register(new DDNSFullName(mWwwName), port );
					mHaveRegistered = true;
				}
			} else {
				Log.w(TAG,  "No httpd.name entry in config file.  Won't try to register name server address.");
			}
		} catch (Exception e) {
			Log.w(TAG , "Couldn't register '" + mWwwName + "': " + e.getMessage());
		}
		Log.i(TAG, "Service started on port " + mNanoService.localPort());
	}
}
