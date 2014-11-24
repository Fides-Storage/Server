package org.fides.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.JsonObjectHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.SSLSocket;

/**
 * @author Niels and Jesse
 *         <p/>
 *         Runnable to create a thread for the handling of a client
 */
public class Client implements Runnable {

	private UserFile userFile;

	private Socket server;

	/**
	 * Constructor for client connection
	 *
	 * @param server socket for the connection with the client
	 */
	public Client(SSLSocket server) {
		this.server = server;
	}

	/**
	 *
	 */
	public void run() {
		DataInputStream in = null;
		DataOutputStream out = null;
		try {
			// Get input from the client
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());

			JsonObject jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);

			String action = JsonObjectHandler.getProperty(jobj, "action");

			if (action.equals("createUser")) { // Create User
				createUser(jobj, out);
			} else if (action.equals("getKeyFile")) { // Get keyfile
				if (authenticateUser(jobj, out)) {
					// TODO
				}
			} else { // else action not found
				JsonObject returnJobj = new JsonObject();
				returnJobj.addProperty("succesfull", false);
				returnJobj.addProperty("error", "action not found");
				out.writeUTF(new Gson().toJson(returnJobj));
			}

			in.close();
			out.close();
			server.close();
		} catch (EOFException e) {
			// Closed by client don't throw a error message
		} catch (IOException e) {
			System.err.println("IOException on server socket listen: " + e);
		}

	}

	/**
	 * Creates a user based on recived json object
	 *
	 * @param jobj
	 * @param out
	 * @throws IOException
	 */
	public void createUser(JsonObject jobj, DataOutputStream out) throws IOException {

		String username = JsonObjectHandler.getProperty(jobj, "username");
		String passwordHash = JsonObjectHandler.getProperty(jobj, "passwordHash");

		JsonObject returnJobj = new JsonObject();

		if (username != null && !username.isEmpty() && passwordHash != null && !passwordHash.isEmpty()) {
			if (!UserManager.checkIfUserExists(username)) {
				UserFile uf = new UserFile(username, passwordHash);
				UserManager.saveUserFile(uf);
				returnJobj.addProperty("succesfull", true);

			} else {
				returnJobj.addProperty("succesfull", false);
				returnJobj.addProperty("error", "username already exists");
			}
		} else {
			returnJobj.addProperty("succesfull", false);
			returnJobj.addProperty("error", "username or password is empty");
		}

		out.writeUTF(new Gson().toJson(returnJobj));

	}

	/**
	 * Authenticate user based on jsonobject with username and password
	 *
	 * @param jobj json object with atleast username and password
	 * @param out  output stream to client to write error message
	 * @return if user is authenticated or not
	 * @throws IOException when trying to write to the client
	 */
	public boolean authenticateUser(JsonObject jobj, DataOutputStream out) throws IOException {
		String username = JsonObjectHandler.getProperty(jobj, "username");
		String passwordHash = JsonObjectHandler.getProperty(jobj, "passwordHash");

		String errorMessage = null;

		if (!username.isEmpty() && !passwordHash.isEmpty()) {
			userFile = UserManager.unlockUserFile(username, passwordHash);
			if (userFile == null) {
				errorMessage = "can't open user file";
			}
		} else {
			errorMessage = "username or password is empty";
		}

		if (!errorMessage.isEmpty()) {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("succesfull", false);
			returnJobj.addProperty("error", errorMessage);
			out.writeUTF(new Gson().toJson(returnJobj));
			return false;
		} else {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("succesfull", true);
			out.writeUTF(new Gson().toJson(returnJobj));
			return true;
		}

	}
}
