package org.fides.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 
 * @author Niels and Jesse
 * 
 *         Runnable to create a thread for the handling of a client
 */
public class ClientConnection implements Runnable {

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
			DataInputStream in = new DataInputStream(server.getInputStream());
			String data = in.readUTF();

			Gson gson = new Gson();
			JsonObject jobj = gson.fromJson(data, JsonObject.class);

			String username = jobj.get("username").toString();
			String passwordHash = jobj.get("password").toString();

			System.out.println("username: " + username + " " + "password: " + passwordHash);

			if (!username.isEmpty() && !passwordHash.isEmpty()) {
				userFile = UserManager.unlockUserFile(username, passwordHash);

				if (userFile != null) {
					// TODO return auth.
				} else {
					// TODO Json error return can't open user file
				}
			} else {
				// TODO return json error to client
			}

			server.close();
		} catch (IOException e) {
			System.out.println("IOException on socket listen: " + e);
			e.printStackTrace();
		}
	}
}
