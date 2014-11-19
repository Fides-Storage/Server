package org.fides.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class App {
	private static int port;

	private static String userDir;

	private static String dataDir;

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		Properties properties = new Properties();
		String propertiesFile = "./config.properties";

		try {
			InputStream in = new FileInputStream(propertiesFile);
			properties.load(in);
			in.close();
			port = Integer.parseInt(properties.getProperty("port"));
			userDir = properties.getProperty("userDir");
			dataDir = properties.getProperty("dataDir");

			System.out.println("Starting up the Fides server on port: " + port);
			System.out.println("Using user directory: " + userDir);
			System.out.println("Using data directory: " + dataDir);
		}
		catch (FileNotFoundException e) {
			System.err.println("Properties file is not found: " + e.getMessage());
			System.exit(1);
		}
		catch (IOException e) {
			System.err.println("IOException has occured: " + e.getMessage());
			System.exit(1);
		}
		
		// Create the userDirectory and the dataDirectory if they don't exist.
		File userFolder = new File(userDir);
		if (!userFolder.exists()) {
			userFolder.mkdirs();
		}
		File dataFolder = new File(dataDir);
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		Server server;
		try {
			server = new Server(port, userDir, dataDir);
			Thread serverThread = new Thread(server);
			serverThread.start();
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}
}
