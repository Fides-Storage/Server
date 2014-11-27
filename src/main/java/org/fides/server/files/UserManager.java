package org.fides.server.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;
import org.fides.server.tools.PropertiesManager;

/**
 * This class manages the users using static functions. It can unlock and save user files.
 * 
 * @author Niels and Jesse
 * 
 */
public final class UserManager {

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
		if (file.exists() && file.isFile()) {
			ObjectInputStream os = null;
			try {
				FileInputStream in = new FileInputStream(file.getPath());

				// TODO: decrypt file

				os = new ObjectInputStream(in);
				UserFile userFile = (UserFile) os.readObject();
				return userFile;

			} catch (FileNotFoundException e) {
				System.err.println("UserFile not found: " + e.getMessage());
			} catch (IOException e) {
				System.err.println("IOException has occured: " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("UserFile was not a UserFile: " + e.getMessage());
			} finally {
				IOUtils.closeQuietly(os);
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
			FileOutputStream fos = new FileOutputStream(new File(PropertiesManager.getInstance()
				.getUserDir(), userFile.getUsername()));

			// TODO: encrypt file

			oos = new ObjectOutputStream(fos);
			oos.writeObject(userFile);

			return true;

		} catch (FileNotFoundException e) {
			System.err.println("UserFile not found: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IOException has occured: " + e.getMessage());
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
		if (userFile.exists() && userFile.isFile()) {
			return true;
		}
		return false;
	}

}
