package org.fides.server.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.fides.server.Server;

/**
 * 
 * @author Niels en Jesse
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
		String fileLocation = Server.getUserDir() + "/" + username;
		File file = new File(fileLocation);
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

	public static boolean saveUserFile(UserFile userFile) {
		return false;
	}

}
