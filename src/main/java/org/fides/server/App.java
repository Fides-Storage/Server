package org.fides.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class App {
	private static final int PORT = 4444;

	/**
	 * Main class of the Fides Server
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(PORT);
			Socket server;

			while (true) {

				server = listener.accept();
				ClientConnection client = new ClientConnection(server);
				Thread t = new Thread(client);
				t.start();
			}
		}
		catch (IOException e) {
			System.out.println("IOException on socket listen: " + e.getMessage());

		}
		finally {

			try {
				listener.close();
			}
			catch (IOException e) {
				System.out.println("IOException on socket listen: " + e.getMessage());

			}
		}
	}
}
