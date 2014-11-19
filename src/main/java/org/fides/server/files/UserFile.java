package org.fides.server.files;

import java.io.Serializable;
import java.util.Collection;
import java.util.GregorianCalendar;

/**
 * 
 * @author Niels en Jesse
 * 
 */
public class UserFile implements Serializable {

	/**
	 * Serializable key
	 */
	private static final long serialVersionUID = 4099951094350728444L;

	private String username;

	private String passwordHash;

	private Collection<String> userFiles;

	private String keyFile;

	private GregorianCalendar lastRefreshed;

	/**
	 * Constructor for the user file
	 * 
	 * @param username
	 *            the given user name
	 * @param passwordHash
	 *            the given password hash
	 */
	public UserFile(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public String getUsername() {
		return username;
	}

	/**
	 * Checks if the given location contains a file belonging to the user
	 * 
	 * @param location
	 *            of the file
	 * @return true if the user contains the file at the given location, false otherwise
	 */
	public boolean checkOwned(String location) {
		return userFiles.contains(location);

	}

	public void addFile(String location) {

	}

	public void removeFile(String location) {
		userFiles.remove(location);
		// FileManager.removeFile(location);
	}

}
