package org.fides.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.fides.server.files.UserFile;

/**
 * 
 * @author Niels and Jesse
 * 
 *         Runnable to create a thread for the handling of a client
 */
public class ClientConnection implements Runnable {

	// TODO
	private UserFile userFile;

	private Socket server;

	/**
	 * Constructor for client connection
	 * 
	 * @param server
	 *            socket for the connection with the client
	 */
	public ClientConnection(Socket server) {
		this.server = server;
	}

	/**
	 * 
	 */
	public void run() {

		try {
			// Get input from the client

			System.out.println("Waiting for client on port " +
				server.getLocalPort() + "...");
			System.out.println("Just connected to "
				+ server.getRemoteSocketAddress());
			DataInputStream in =
				new DataInputStream(server.getInputStream());
			System.out.println(in.readUTF());
			DataOutputStream out =
				new DataOutputStream(server.getOutputStream());
			out.writeUTF("Thank you for connecting to "
				+ server.getLocalSocketAddress() + "\nGoodbye!");

			server.close();
		}
		catch (IOException ioe) {
			System.out.println("IOException on socket listen: " + ioe);
			ioe.printStackTrace();
		}
	}
}
