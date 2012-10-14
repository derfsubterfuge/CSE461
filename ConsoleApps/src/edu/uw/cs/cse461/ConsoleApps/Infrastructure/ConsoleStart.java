package edu.uw.cs.cse461.ConsoleApps.Infrastructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetBaseConsole;
import edu.uw.cs.cse461.Net.Base.NetBaseInterface;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.Log;

/**
 * This class implements OS startup for the Console environment.
 * Execution in the Console environment begins in main() of this class.
 * @author zahorjan
 *
 */
public abstract class ConsoleStart implements NetBaseInterface {
	
	/**
	 * A simple driver that fires up the OS by calling its boot() method.
	 * We need to pass boot() a FileInputStream, connected to the config file to use,
	 * because of restrictions of the Android implementation.
	 * @param args
	 */
	public static void main(String[] args) {
		final String TAG="ConsoleStart";
		String configDir = ".";
		File configFile = null;
		
		try {
			// This code deals with command line options
			Options options = new Options();
			options.addOption("d", "configdir", true, "Config file directory (Default: " + configDir + ")");
			options.addOption("f", "configfile", true, "Path name of config file");
			options.addOption("h", "hostname", true, "Specify hostname.  (Overrides any value in config file.)");
			options.addOption("H", "help", false, "Print this message");

			CommandLineParser parser = new PosixParser();

			CommandLine line = parser.parse(options, args);
			if ( line.hasOption("help") ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java ConsoleStart", options );
				return;
			}
			
			if ( line.hasOption("configfile") ) {
				configFile = new File(line.getOptionValue("configfile"));
			}
			else {
				if ( line.hasOption("configdir")) configDir = line.getOptionValue("configdir"); 

				File dir = new File(line.getOptionValue("configdir"));
				if ( !dir.isDirectory()) {
					System.err.println(line.getOptionValue("configdir") + " isn't a directory");
					System.exit(-1);
				}

				// enumerate *.config.ini files in the directory and let the user choose one
				FilenameFilter configFilter = new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.endsWith("config.ini");
					}
				};
				File[] fileList = dir.listFiles(configFilter);
				if ( fileList.length == 1) {
					// if there's only one, just use it without asking
					configFile = fileList[0];
				} else if ( fileList.length > 0 ){
					BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
					// let user choose
					do {
						System.out.println("Choose a configuration file:");
						for ( int i=0; i<fileList.length; i++ ) {
							System.out.println( String.format("\t%2d  %s", i, fileList[i].getName()));
						}
						System.out.print("Enter choice> ");
						String choice = console.readLine();
						if ( choice.isEmpty()) return;  // terminate if no choice made
						int selection = Integer.parseInt(choice);
						configFile = fileList[selection];
					} while ( configFile == null );
				}
			}
			
			if ( configFile == null ) {
				System.err.println("Can't identify a configuration file.  Try using a relevant command line option.");
				System.err.println("(Process working directory is " + new File(".").getCanonicalPath() + ")");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java ConsoleStart", options, false );
				System.exit(-1);
			}
			
			if (!configFile.isFile() ) {
				System.err.println("Config file " + configFile.getCanonicalPath() + " doesn't exist");
				System.exit(-1);
			}
			
			if (!configFile.canRead() ) {
				System.err.println("Can't read config file " + configFile.getCanonicalPath() );
				System.exit(-1);
			}
			
			Log.w(TAG, "Config filename = " + configFile.getCanonicalPath());

			// try to read configuration file
			ConfigManager configMgr = new ConfigManager(new FileInputStream(configFile));
			
			// Override the config's net.hostname property with the hostname specified as a command line argument
			if ( line.hasOption("hostname") ) configMgr.setProperty("net.hostname", line.getOptionValue("hostname"));
			
			// Save the directory we found the config file in in the config itself.  (Used by DDNSService to locate
			// ddns.nodefile.)
			configMgr.setProperty("config.directory", configFile.getParent() );
			
			NetBaseConsole theNetBase = new NetBaseConsole();
			theNetBase.init(configMgr);
			
			// Load all console applications and then start the initial application.
			theNetBase.loadApps();
			// if there is an console.initialapp app, start it. Otherwise, just run as a daemon
			String initialAppName = NetBase.theNetBase().config().getProperty("console.initialapp");
			if (initialAppName != null) {
				try {
					NetBase.theNetBase().startApp(initialAppName);
				} catch (Exception e) {
					Log.e(TAG,	initialAppName + " threw exception: " + e.getMessage());
				}
				NetBase.theNetBase().shutdown(); // we're done when the inital app terminates
			}
			else {
				Log.w(TAG, "Possible config file bug -- No console.initialapp entry.");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Caught " + e.getClass().getName() + " exception: " + e.getMessage());
		}
	}
	
}
