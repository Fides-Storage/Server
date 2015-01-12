package org.fides.server;

import java.util.Timer;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.server.tools.CleanerTask;

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

		LOG.trace("Starting timer for cleaner");
		Timer timer = new Timer("CleanerTimer");
		long timeToClean = TimeUnit.DAYS.toMillis(1);
		timer.scheduleAtFixedRate(new CleanerTask(), 0, timeToClean);

	}
}
