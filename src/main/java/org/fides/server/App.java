package org.fides.server;

import java.io.IOException;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class App {

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {

		Server server;
		try {
			server = new Server();
			Thread serverThread = new Thread(server);
			serverThread.start();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}
}
