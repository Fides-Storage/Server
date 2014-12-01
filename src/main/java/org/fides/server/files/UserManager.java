package org.fides.server.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.server.tools.FileUtil;
import org.fides.server.tools.PropertiesManager;

/**
 * This class manages the users using static functions. It can unlock and save user files.
 * 
 * @author Niels and Jesse
 * 
 */
public final class UserManager {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserManager.class);

	/**
	 * Opens the user file based on the user name and decrypts it based on the password hash
	 * 
	 * @param username
	 *            the given user name
	 * @param passwordHash
	 *            the given password hash
	 * @return the user file
	 */
	public static UserFile unlockUserFile(String username, String passwordHash) {
		File file = new File(PropertiesManager.getInstance().getUserDir(), username);

		// username is not in folder
		if (!FileUtil.isInFolder(new File(PropertiesManager.getInstance().getUserDir()), file)) {
			return null;
		}

		if (file.exists() && file.isFile()) {
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
	 * @param userFile
	 *            the user file based on the user name
	 * @return true if succeeded, false otherwise
	 */
	public static boolean saveUserFile(UserFile userFile) {
		ObjectOutputStream oos = null;
		try {
			File userLocation = new File(PropertiesManager.getInstance().getUserDir());
			File userFileLocation = new File(userLocation, userFile.getUsername());

			if (FileUtil.isInFolder(userLocation, userFileLocation)) {
				FileOutputStream fos = new FileOutputStream(userFileLocation);

				// TODO: encrypt file

				oos = new ObjectOutputStream(fos);
				oos.writeObject(userFile);

				return true;
			}

		} catch (FileNotFoundException e) {
			log.error("UserFile not found", e);
		} catch (IOException e) {
			log.error("IOException has occured", e);
		} finally {
			IOUtils.closeQuietly(oos);
		}

		return false;
	}

	/**
	 * Checks if user name exists
	 * 
	 * @param username
	 *            the given user name
	 * @return username exists or not
	 */
	public static boolean checkIfUserExists(String username) {
		File userFile = new File(PropertiesManager.getInstance().getUserDir(), username);

		// username is not in folder
		if (!FileUtil.isInFolder(new File(PropertiesManager.getInstance().getUserDir()), userFile)) {
			return false;
		}

		if (userFile.exists() && userFile.isFile()) {
			return true;
		}

		return false;
	}

}
