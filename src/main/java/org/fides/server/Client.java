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
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.files.UserFile;
import org.fides.server.files.UserManager;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
	 * Runnable for client connection
	 */
	public void run() {
		UserFile userFile = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		JsonObject requestObject;
		try {
			// Get input from the client
			in = new DataInputStream(server.getInputStream());
			out = new DataOutputStream(server.getOutputStream());

			// While user is not logged in
			while (userFile == null) {
				requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);

				String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);

				switch (action) {
				case Actions.CREATEUSER:
					UserManager.createUser(requestObject, out);
					break;
				case Actions.LOGIN:
					userFile = UserManager.authenticateUser(requestObject, out);
					break;
				default:
					// TODO: Use the copyErrorToStream function that's currently in ClientFileConnector
					JsonObject returnJobj = new JsonObject();
					returnJobj.addProperty(Responses.SUCCESSFUL, false);
					returnJobj.addProperty(Responses.ERROR, Errors.UNKNOWNACTION);
					out.writeUTF(new Gson().toJson(returnJobj));
					break;
				}
			}

			ClientFileConnector clientFileConnector = new ClientFileConnector(userFile);

			requestObject = new Gson().fromJson(in.readUTF(), JsonObject.class);

			String action = JsonObjectHandler.getProperty(requestObject, Actions.ACTION);

			switch (action) {
			case Actions.GETKEYFILE:
				clientFileConnector.downloadKeyFile(out);
				break;
			case Actions.GETFILE:
				clientFileConnector.downloadFile(requestObject, out);
				break;
			case Actions.UPDATEKEYFILE:
				clientFileConnector.updateKeyFile(in, out);
				break;
			case Actions.UPDATEFILE:
				clientFileConnector.updateFile(in, requestObject, out);
				break;
			case Actions.UPLOADFILE:
				clientFileConnector.uploadFile(in, out);
				break;
			case Actions.REMOVEFILE:
				clientFileConnector.removeFile(requestObject, out);
				break;
			default:
				JsonObject returnJobj = new JsonObject();
				returnJobj.addProperty(Responses.SUCCESSFUL, false);
				returnJobj.addProperty(Responses.ERROR, Errors.UNKNOWNACTION);
				out.writeUTF(new Gson().toJson(returnJobj));
				out.close();
				break;
			}

		} catch (EOFException e) {
			log.debug("Closed by client don't throw an error message");
		} catch (IOException e) {
			log.error("IOException on server socket listen", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(server);
			// TODO: Unlock and close userFile
		}
	}
}
