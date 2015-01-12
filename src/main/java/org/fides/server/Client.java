package org.fides.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.CommunicationUtil;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;
import org.fides.server.tools.UserLocker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Runnable to create a thread for the handling of a client
 * 
 */
public class Client implements Runnable {

	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(Client.class);

	private final Socket server;

	private UserFile userFile;

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
	 * Runnable for client connection
	 */
	public void run() {
		JsonObject requestObject;
		try (DataInputStream in = new DataInputStream(server.getInputStream());
			DataOutputStream out = new DataOutputStream(server.getOutputStream())) {
			// While user is not logged in
			while (userFile == null) {
				requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);

				String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);

				switch (action) {
				case Actions.CREATE_USER:
					UserManager.createUser(requestObject, out);
					break;
				case Actions.LOGIN:
					userFile = UserManager.authenticateUser(requestObject, out);
					break;
				case Actions.DISCONNECT:
					return;
				default:
					CommunicationUtil.returnError(out, Errors.UNKNOWN_ACTION);
					break;
				}
			}

			// Update timestamp of all files
			userFile.touch();

			// Start the reading and handling of user actions.
			ClientFileConnector clientFileConnector = new ClientFileConnector(userFile);
			handleActions(in, clientFileConnector, out);

		} catch (EOFException e) {
			LOG.debug("Closed by client don't throw an error message");
		} catch (IOException e) {
			LOG.error("IOException on server socket listen", e);
		} finally {
			IOUtils.closeQuietly(server);
		}
	}

	/**
	 * Keeps listening to actions from the client and handles them. Will stop listening when it receives a disconnect.
	 * 
	 * @param in
	 *            The InputStream with input from the client
	 * @param clientFileConnector
	 *            The ClientFileConnector of the user that's logged in.
	 * @param out
	 *            The OutputStream to send output to the client
	 * @throws EOFException
	 * @throws IOException
	 */
	public void handleActions(DataInputStream in, ClientFileConnector clientFileConnector, DataOutputStream out) throws IOException {
		try {
			JsonObject requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);
			String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);
			while (!action.equals(Actions.DISCONNECT)) {
				LOG.trace("Action: " + action);

				switch (action) {
				case Actions.GET_KEY_FILE:
					clientFileConnector.downloadKeyFile(out);
					break;
				case Actions.GET_FILE:
					clientFileConnector.downloadFile(requestObject, out);
					break;
				case Actions.UPDATE_KEY_FILE:
					clientFileConnector.updateKeyFile(in, out);
					break;
				case Actions.UPDATE_FILE:
					clientFileConnector.updateFile(in, requestObject, out);
					break;
				case Actions.UPLOAD_FILE:
					clientFileConnector.uploadFile(in, out);
					break;
				case Actions.REMOVE_FILE:
					clientFileConnector.removeFile(requestObject, out);
					break;
				default:
					CommunicationUtil.returnError(out, Errors.UNKNOWN_ACTION);
					out.close();
					break;
				}
				requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);
				action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);
			}
		} catch (SocketException e) {
			LOG.debug("Closed by client don't throw an error message");
		} finally {
			UserLocker.unlock(userFile.getUsernameHash());
		}

		LOG.trace("Action: " + Actions.DISCONNECT);
	}
}
