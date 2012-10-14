package edu.uw.cs.cse461.Net.Base;

/**
 * Because of some difficulties supporting both console and Android implementations via
 * a common base class, we end up with both a NetLoadable class and an interface.
 *
 * @author zahorjan
 *
 */
public interface NetLoadableInterface {
	
	public boolean isImplemented();
	public String loadablename();
	
	public interface NetLoadableServiceInterface extends NetLoadableInterface {
		public void shutdown();
		public String dumpState();
	}

	public interface NetLoadableConsoleAppInterface extends NetLoadableInterface {
		public void run() throws Exception;
		public void shutdown();
	}
	
	public interface NetLoadableAndroidAppInterface extends NetLoadableInterface {
	}
}
