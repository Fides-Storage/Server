package org.fides.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class App {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(App.class);

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		Server server;
		log.trace("Starting server");
		try {
			server = new Server();
			Thread serverThread = new Thread(server);
			serverThread.start();
		} catch (IOException e) {
			log.error(e);
		}

	}
}
