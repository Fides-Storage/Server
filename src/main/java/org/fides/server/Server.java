package org.fides.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

	private ServerSocket listener;

	private volatile boolean isRunning = true;

	public Server(int port) throws IOException {
		listener = new ServerSocket(port);
	}

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

	public void kill() {
		isRunning = false;
	}
}
