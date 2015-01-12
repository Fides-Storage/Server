package org.fides.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main app to run
 */
public class App {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(App.class);

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		Server server;
		LOG.trace("Starting server");
		server = new Server();
		Thread serverThread = new Thread(server);
		serverThread.start();

	}
}
