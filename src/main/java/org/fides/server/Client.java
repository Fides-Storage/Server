package org.fides.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.JsonObjectHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Runnable to create a thread for the handling of a client
 * 
 * @author Niels and Jesse
 *
 */
public class Client implements Runnable {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(Client.class);

	private UserFile userFile;

	private Socket server;

	/**
	 * Constructor for client connection
	 * 
	 * @param server
	 *            socket for the connection with the client
	 */
	public Client(SSLSocket server) {
		this.server = server;
	}

	/**
	 * TODO: Javadoc
	 */
	public void run() {
		DataInputStream in = null;
		DataOutputStream out = null;
		JsonObject jobj;
		try {
			// Get input from the client
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());

			jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);

			String action = JsonObjectHandler.getProperty(jobj, "action");

			// first action needs to be create user or login
			//TODO: Prevent NullpointerException on action
			if (action.equals("createUser")) { // Create User
				createUser(jobj, out);
			} else if (action.equals("login")) { // Login User
				authenticateUser(jobj, out);
			} else { // else action not found
				JsonObject returnJobj = new JsonObject();
				returnJobj.addProperty("successful", false);
				returnJobj.addProperty("error", "action not found");
				out.writeUTF(new Gson().toJson(returnJobj));
			}

			// While client is logged in
			while (userFile != null) {
				jobj = new Gson().fromJson(in.readUTF(), JsonObject.class);

				action = JsonObjectHandler.getProperty(jobj, "action");

				if (action.equals("getKeyFile")) { // Get Key file
					// TODO: return keyFile
				} else { // else action not found
					JsonObject returnJobj = new JsonObject();
					returnJobj.addProperty("successful", false);
					returnJobj.addProperty("error", "action not found");
					out.writeUTF(new Gson().toJson(returnJobj));
				}
			}

		} catch (EOFException e) {
			log.debug("Closed by client don't throw an error message");
		} catch (IOException e) {
			log.error("IOException on server socket listen", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(server);
			// TODO: unlock and close userFile
		}
	}

	/**
	 * Creates a user based on received json object
	 * 
	 * @param jobj
	 * @param out
	 * @throws IOException
	 * TODO: Javadoc (explain parameters), more selfexplaining varnames (ex. 'userObject' instead of 'jobj')
	 */
	public void createUser(JsonObject jobj, DataOutputStream out) throws IOException {

		String username = JsonObjectHandler.getProperty(jobj, "username");
		String passwordHash = JsonObjectHandler.getProperty(jobj, "passwordHash");

		JsonObject returnJobj = new JsonObject();

		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(passwordHash)) {
			if (!UserManager.checkIfUserExists(username)) {
				UserFile uf = new UserFile(username, passwordHash);
				UserManager.saveUserFile(uf);
				returnJobj.addProperty("successful", true);

			} else {
				returnJobj.addProperty("successful", false);
				returnJobj.addProperty("error", "username already exists");
			}
		} else {
			returnJobj.addProperty("successful", false);
			returnJobj.addProperty("error", "username or password is empty");
		}

		out.writeUTF(new Gson().toJson(returnJobj));

	}

	/**
	 * Authenticate user based on jsonobject with username and password
	 * 
	 * @param jobj
	 *            json object with at least username and password
	 * @param out
	 *            output stream to client to write error message
	 * @return if user is authenticated or not
	 * @throws IOException
	 *             when trying to write to the client
	 * TODO: More selfexplaining varnames (ex. 'userObject' instead of 'jobj')            
	 */
	public boolean authenticateUser(JsonObject jobj, DataOutputStream out) throws IOException {
		String username = JsonObjectHandler.getProperty(jobj, "username");
		String passwordHash = JsonObjectHandler.getProperty(jobj, "passwordHash");

		String errorMessage = null;

		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(passwordHash)) {
			userFile = UserManager.unlockUserFile(username, passwordHash);
			if (userFile == null) {
				errorMessage = "Username or password is incorrect";
			}
		} else {
			errorMessage = "Username or password is empty";
		}

		if (StringUtils.isNotBlank(errorMessage)) {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("successful", false);
			returnJobj.addProperty("error", errorMessage);
			out.writeUTF(new Gson().toJson(returnJobj));
			return false;
		} else {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty("successful", true);
			out.writeUTF(new Gson().toJson(returnJobj));
			return true;
		}

	}
}
