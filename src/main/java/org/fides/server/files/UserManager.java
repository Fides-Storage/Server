package org.fides.server.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.fides.server.tools.PropertiesManager;

/**
 * 
 * @author Niels and Jesse
 * 
 */
public class UserManager {

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
			try {
				FileInputStream in = new FileInputStream(file.getPath());

				// TODO decrypt file

				ObjectInputStream os = new ObjectInputStream(in);
				UserFile uf = (UserFile) os.readObject();
				os.close();
				return uf;

			} catch (FileNotFoundException e) {
				System.err.println("UserFile not found: " + e.getMessage());
			} catch (IOException e) {
				System.err.println("IOException has occured: " + e.getMessage());
			} catch (ClassNotFoundException e) {
				System.err.println("UserFile was not a UserFile: " + e.getMessage());
			}
		}
		return null;
	}

	/**
	 * Saves the user file in the user directory
	 * 
	 * @param userFile
	 *            the user file based on the user name
	 * @return true if succeeded, false otherwise
	 */
	public static boolean saveUserFile(UserFile userFile) {
		try {
			FileOutputStream fos = new FileOutputStream(new File(PropertiesManager.getInstance().getUserDir(), userFile.getUsername()));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(userFile);
			oos.close();
			return true;

		} catch (FileNotFoundException e) {
			System.err.println("UserFile not found: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IOException has occured: " + e.getMessage());
		}

		return false;
	}

}
