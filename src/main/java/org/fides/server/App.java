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
		drawLogo();
		LOG.trace("Starting server");
		server = new Server();
		Thread serverThread = new Thread(server);
		serverThread.start();

		LOG.trace("Starting timer for cleaner");
		Timer timer = new Timer("CleanerTimer");
		long timeToClean = TimeUnit.DAYS.toMillis(1);
		timer.scheduleAtFixedRate(new CleanerTask(), 0, timeToClean);

	}

	private static void drawLogo() {
		LOG.info("\n"
			+ "                                          dddddddd                                     \n"
			+ "FFFFFFFFFFFFFFFFFFFFFF  iiii              d::::::d                                     \n"
			+ "F::::::::::::::::::::F i::::i             d::::::d                                     \n"
			+ "F::::::::::::::::::::F  iiii              d::::::d                                     \n"
			+ "FF::::::FFFFFFFFF::::F                    d:::::d                                      \n"
			+ "  F:::::F       FFFFFFiiiiiii     ddddddddd:::::d     eeeeeeeeeeee        ssssssssss   \n"
			+ "  F:::::F             i:::::i   dd::::::::::::::d   ee::::::::::::ee    ss::::::::::s  \n"
			+ "  F::::::FFFFFFFFFF    i::::i  d::::::::::::::::d  e::::::eeeee:::::eess:::::::::::::s \n"
			+ "  F:::::::::::::::F    i::::i d:::::::ddddd:::::d e::::::e     e:::::es::::::ssss:::::s\n"
			+ "  F:::::::::::::::F    i::::i d::::::d    d:::::d e:::::::eeeee::::::e s:::::s  ssssss \n"
			+ "  F::::::FFFFFFFFFF    i::::i d:::::d     d:::::d e:::::::::::::::::e    s::::::s      \n"
			+ "  F:::::F              i::::i d:::::d     d:::::d e::::::eeeeeeeeeee        s::::::s   \n"
			+ "  F:::::F              i::::i d:::::d     d:::::d e:::::::e           ssssss   s:::::s \n"
			+ "FF:::::::FF           i::::::id::::::ddddd::::::dde::::::::e          s:::::ssss::::::s\n"
			+ "F::::::::FF           i::::::i d:::::::::::::::::d e::::::::eeeeeeee  s::::::::::::::s \n"
			+ "F::::::::FF           i::::::i  d:::::::::ddd::::d  ee:::::::::::::e   s:::::::::::ss  \n"
			+ "FFFFFFFFFFF           iiiiiiii   ddddddddd   ddddd    eeeeeeeeeeeeee    sssssssssss    \n");
	}
}
