package org.fides.server.files;

import java.io.File;
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

	private Collection<File> userFiles;

	private File keyFile;

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

	public boolean checkOwned(String location) {
		return false;
	}

	public void addFile(String location) {

	}

	public void removeFile(String location) {

	}

}
