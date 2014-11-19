package org.fides.server;

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

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		Properties properties = new Properties();
		String propertiesFile = "config.properties";
		try {
			InputStream in = new FileInputStream(propertiesFile);
			properties.load(in);
			port = Integer.parseInt(properties.getProperty("port"));
		}
		catch (FileNotFoundException e) {
			System.exit(1);
		}
		catch (IOException e) {
			System.exit(1);
		}
		finally {

		}

		Server server;
		try {
			server = new Server(port);
			Thread serverThread = new Thread(server);
			serverThread.start();
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}
}
