package edu.uw.cs.cse461.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPFinder {
	public static String getMyIP() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "Unknown-host";
		}
	}
}
