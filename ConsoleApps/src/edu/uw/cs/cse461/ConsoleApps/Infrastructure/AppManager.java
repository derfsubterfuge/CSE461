package edu.uw.cs.cse461.ConsoleApps.Infrastructure;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadable.NetLoadableConsoleApp;

/**
 * An AppManager acts someting like a shell on a traditional system - it lets
 * you run apps (OSConsoleApps).  Unlike traditional systems, but something
 * like Android, there is only one instance of an app created, no matter how
 * many times it's invoked.  In this system, all apps are loaded when the 
 * OS boots.  An app invocation is merely a call to its run() method.
 * The AppManager   
 * @author zahorjan
 *
 */
public class AppManager extends NetLoadableConsoleApp {
	
	public void shutdown() {}
	
	/**
	 * Constructor required by OSConsoleApp.
	 * 
	 * @param args String[] argument passed to main().
	 * @throws Exception
	 */
	public AppManager() {
		super("appmanager", true);
	}

	/**
	 * This method implements a very primitive shell.  Apps are "run in the foreground."
	 * @throws Exception
	 */
	public void run() throws Exception {
		
		// Eclipse doesn't support System.console()
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		
		// convert list of loaded apps into a prompt string
		List<String> appList = NetBase.theNetBase().loadedAppNames();
		StringBuilder sb = new StringBuilder().append("Enter app name (");
		String name;
		for ( int i=0; i<appList.size()-1; i++) {
			name = appList.get(i);
			sb.append(name).append(", ");
		}
		sb.append(appList.get(appList.size()-1)).append("), or exit: "); 
		String prompt = sb.toString();

		// sit in loop reading user input and executing apps
		while (true) {
			System.out.print(prompt);
			String appName = console.readLine();

			if ( appName == null || appName.trim().isEmpty()) continue;
			if ( appName.equals("exit") ) break;

			try {
				NetBase.theNetBase().startApp(appName);
			} catch (Exception e) {
				System.out.println("App threw uncaught exception: " + e.getMessage());
			}
		}
	}
}
