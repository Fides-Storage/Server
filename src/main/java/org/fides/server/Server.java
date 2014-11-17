package org.fides.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author Niels en Jesse
 * 
 */
public class Server implements Runnable {

	private ServerSocket listener;

	private volatile boolean isRunning = true;

	/**
	 * Constructor to create a new server socket
	 * 
	 * @param port
	 *            port number
	 * @throws IOException
	 *             Throws an IOException if the connection can't be made
	 */
	public Server(int port) throws IOException {
		listener = new ServerSocket(port);
	}

	/**
	 * Accepting client connections
	 */
	public void run() {

		try {

			Socket server;

			while (isRunning) {

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

	/**
	 * Kills the running thread
	 */
	public void kill() {
		isRunning = false;
	}
}
