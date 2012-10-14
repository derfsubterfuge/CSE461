package edu.uw.cs.cse461.Net.DDNS;

/**
 * The DDNSService is invoked only via RPC (even if a ddns resolver and server reside on a single node).
 * There are therefore no DDNS-specific methods in this interface.
 */
public interface DDNSServiceInterface {
	public String toString();
}
