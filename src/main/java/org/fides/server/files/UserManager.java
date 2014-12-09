package org.fides.server.files;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.components.Actions;
import org.fides.components.Responses;
import org.fides.server.tools.Errors;
import org.fides.server.tools.JsonObjectHandler;
import org.fides.server.tools.PropertiesManager;

/**
 * This class manages the users using static functions. It can unlock and save user files.
 *
 * @author Niels and Jesse
 */
public final class UserManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserManager.class);

	/**
	 * Opens the user file based on the user name and decrypts it based on the password hash
	 *
	 * @param username     the given user name
	 * @param passwordHash the given password hash
	 * @return the user file
	 */
	public static UserFile unlockUserFile(String username, String passwordHash) {
		File file = new File(PropertiesManager.getInstance().getUserDir(), username);

		// Check if the username is in the folder
		if (checkIfUserExists(username)) {
			ObjectInputStream userFileObject = null;
			try {
				FileInputStream in = new FileInputStream(file.getPath());

				// TODO: decrypt file

				userFileObject = new ObjectInputStream(in);
				UserFile userFile = (UserFile) userFileObject.readObject();

				if (userFile.checkPasswordHash(passwordHash)) {
					return userFile;
				}

			} catch (FileNotFoundException e) {
				log.debug("UserFile not found for username: " + username);
			} catch (IOException e) {
				log.error("IOException has occured", e);
			} catch (ClassNotFoundException e) {
				log.error("UserFile was not a UserFile", e);
			} finally {
				IOUtils.closeQuietly(userFileObject);
			}
		}
		return null;
	}

	/**
	 * Encrypts the user file and saves it in the user directory
	 *
	 * @param userFile the user file based on the user name
	 * @return true if succeeded, false otherwise
	 */
	public static boolean saveUserFile(UserFile userFile) {
		ObjectOutputStream out = null;
		try {
			File userFileLocation = new File(PropertiesManager.getInstance().getUserDir(), userFile.getUsername());

			if (userFileLocation.getName().equals(userFile.getUsername())) {
				FileOutputStream fos = new FileOutputStream(userFileLocation);

				// TODO: encrypt file

				out = new ObjectOutputStream(fos);
				out.writeObject(userFile);

				return true;
			}

		} catch (FileNotFoundException e) {
			log.error("UserFile not found", e);
		} catch (IOException e) {
			log.error("IOException has occured", e);
		} finally {
			IOUtils.closeQuietly(out);
		}

		return false;
	}

	/**
	 * Checks if user name exists
	 *
	 * @param username the given user name
	 * @return username exists or not
	 */
	public static boolean checkIfUserExists(String username) {
		File userFile = new File(PropertiesManager.getInstance().getUserDir(), username);

		// Check if username is in the folder
		return userFile.exists() && userFile.getName().equals(username) && userFile.isFile();
	}



	/**
	 * Creates a user based on received json object
	 *
	 * @param userObject
	 *            jsonObject containing username and password
	 * @param out
	 *            outputstream to the client
	 * @throws IOException
	 *             if failed to write to outputstream
	 */
	public static void createUser(JsonObject userObject, DataOutputStream out) throws IOException {
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
	 * @param userObject
	 *            json object with at least username and password
	 * @param out
	 *            output stream to client to write error message
	 * @return if user is authenticated or not
	 */
	public static UserFile authenticateUser(JsonObject userObject, DataOutputStream out) throws IOException {
		UserFile userFile = null;
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
		} else {
			JsonObject returnJobj = new JsonObject();
			returnJobj.addProperty(Responses.SUCCESSFUL, true);
			out.writeUTF(new Gson().toJson(returnJobj));
		}
		return userFile;
	}

}
