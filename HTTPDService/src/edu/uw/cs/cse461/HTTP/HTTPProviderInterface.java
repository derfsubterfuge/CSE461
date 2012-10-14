package edu.uw.cs.cse461.HTTP;

public interface HTTPProviderInterface {
	/**
	 * The interface for components that are accessible through the web server component.
	 * URLs accessing such services specify the serice name as the first component of the url, which is
	 * used by the infrastructure to dispatch the HTTP GET to the correct HTTPProvider.  The remainder of the
	 * URL is provided to the service's httpServe() method, which interprets it in a service-specific way. 
	 *  
	 * @param uriArray uriArray[0] is empty; uriArray[1] is service name (e.g., "ddns"); successive elements are some uri path specific to the service 
	 * @return an HTML page
	 * @throws Exception
	 */
	public String httpServe(String[] uriArray) throws Exception;
}

