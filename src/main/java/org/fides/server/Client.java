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
import org.fides.server.tools.Actions;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.fides.server.tools.Responses;

/**
 * Runnable to create a thread for the handling of a client
 *
 * @author Niels
 * @author Jesse
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
	 * @param server socket for the connection with the client
	 */
	public Client(SSLSocket server) {
		this.server = server;
	}

	/**
	 * Runnable for client connection
	 */
	public void run() {
		DataInputStream in = null;
		DataOutputStream out = null;
		JsonObject requestObject;
		try {
			// Get input from the client
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());

			// first action needs to be create user or login
			while (userFile == null) {
				requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);

				String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);

				switch (action) {
				case Actions.CREATEUSER:
					createUser(requestObject, out);
					break;
				case Actions.LOGIN:
					authenticateUser(requestObject, out);
					break;
				default:
					//TODO: ombouwen naar returnfunctie van Thijs
					JsonObject returnJobj = new JsonObject();
					returnJobj.addProperty(Responses.SUCCESSFUL, false);
					returnJobj.addProperty(Responses.ERROR, Errors.UNKNOWNACTION);
					out.writeUTF(new Gson().toJson(returnJobj));
					break;
				}
			}

			// While client is logged in
			while (userFile != null) {
				requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);

				String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);

				switch (action) {
				case Actions.GETKEYFILE:
					// TODO: return keyFile
					break;
				default:
					JsonObject returnJobj = new JsonObject();
					returnJobj.addProperty(Responses.SUCCESSFUL, false);
					returnJobj.addProperty(Responses.ERROR, Errors.UNKNOWNACTION);
					out.writeUTF(new Gson().toJson(returnJobj));
					break;
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
	 * @param userObject jsonObject containing username and password
	 * @param out        outputstream to the client
	 * @throws IOException if failed to write to outputstream
	 */
	public void createUser(JsonObject userObject, DataOutputStream out) throws IOException {

		String username = JsonObjectHandler.getProperty(userObject, Actions.Properties.USERNAME);
		String passwordHash = JsonObjectHandler.getProperty(userObject, Actions.Properties.PASSWORD_HASH);

		JsonObject returnJobj = new JsonObject();

		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(passwordHash)) {
			if (UserManager.checkIfUserExists(username)) {
				returnJobj.addProperty(Responses.SUCCESSFUL, false);
				returnJobj.addProperty(Responses.ERROR, Errors.USNERNAMEEXISTS);

			} else {
				UserFile uf = new UserFile(username, passwordHash);
				if (UserManager.saveUserFile(uf)) {
					returnJobj.addProperty(Responses.SUCCESSFUL, true);
				} else {
					returnJobj.addProperty(Responses.SUCCESSFUL, false);
					returnJobj.addProperty(Responses.ERROR, Errors.CANNOTSAVEUSERFILE);
				}
			}
		} else {
			returnJobj.addProperty(Responses.SUCCESSFUL, false);
			returnJobj.addProperty(Responses.ERROR, Errors.USERNAMEORPASSWORDEMPTY);
		}

		out.writeUTF(new Gson().toJson(returnJobj));

	}

	/**
	 * Authenticate user based on jsonobject with username and password
	 *
	 * @param userObject json object with at least username and password
	 * @param out        output stream to client to write error message
	 * @return if user is authenticated or not
	 * @throws IOException when trying to write to the client
	 */
	public boolean authenticateUser(JsonObject userObject, DataOutputStream out) throws IOException {
		String username = JsonObjectHandler.getProperty(userObject, Actions.Properties.USERNAME);
		String passwordHash = JsonObjectHandler.getProperty(userObject, Actions.Properties.PASSWORD_HASH);

		String errorMessage = null;

		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(passwordHash)) {
			userFile = UserManager.unlockUserFile(username, passwordHash);
			if (userFile == null) {
				errorMessage = Errors.USERNAMEORPASSWORDINCORRECT;
			}
		} else {
			errorMessage = Errors.USERNAMEORPASSWORDEMPTY;
		}

		if (StringUtils.isNotBlank(errorMessage)) {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, false);
			returnJobj.addProperty(Responses.ERROR, errorMessage);
			out.writeUTF(new Gson().toJson(returnJobj));
			return false;
		} else {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, true);
			out.writeUTF(new Gson().toJson(returnJobj));
			return true;
		}

	}
}
