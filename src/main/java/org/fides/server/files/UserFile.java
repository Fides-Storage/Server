package org.fides.server.files;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for keeping track of the files that belong to a user.
 * 
 */
public class UserFile implements Serializable {

	/**
	 * Serializable key
	 */
	private static final long serialVersionUID = 4099951094350728444L;

	private String usernameHash;

	private String passwordHash;

	private Set<String> userFiles = new HashSet<>();

	private String keyFile;

	private GregorianCalendar lastRefreshed;

	/**
	 * Constructor for the user file
	 * 
	 * @param usernameHash
	 *            the given user name
	 * @param passwordHash
	 *            the given password hash
	 */
	public UserFile(String usernameHash, String passwordHash) {
		this.usernameHash = usernameHash;
		this.passwordHash = passwordHash;
		this.keyFile = FileManager.createFile();
		this.lastRefreshed = new GregorianCalendar();
	}

	public String getUsernameHash() {
		return usernameHash;
	}

	/**
	 * Get the passwordhash
	 * 
	 * @return the passwordhash
	 */
	public String getPasswordHash() {
		return passwordHash;
	}

	/**
	 * Checks if given passwordHash matches passwordHash in file
	 * 
	 * @param testPasswordHash
	 *            given password hash to check
	 * @return true if equals
	 */
	public boolean checkPasswordHash(String testPasswordHash) {
		return passwordHash.equals(testPasswordHash);
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

	/**
	 * Adds file location to the list of files and saves itself
	 * 
	 * @param location
	 *            the location of the new file
	 */
	public void addFile(String location) {
		userFiles.add(location);
		UserManager.saveUserFile(this);
	}

	/**
	 * Removes the file at the given location and saves itself
	 * 
	 * @param location
	 *            the location of the removed file
	 */
	public void removeFile(String location) {
		userFiles.remove(location);
		UserManager.saveUserFile(this);
	}

	/**
	 * Returns the location of the user's keyfile.
	 * 
	 * @return The location of the keyfile.
	 */
	public String getKeyFileLocation() {
		return keyFile;
	}

	/**
	 * Sets the location of the user's keyfile to the userfile.
	 * 
	 * @param location
	 *            The location of the keyfile.
	 */
	public void setKeyFileLocation(String location) {
		keyFile = location;
	}

	public GregorianCalendar getLastRefreshed() {
		return lastRefreshed;
	}

	/**
	 * Sets the last refreshed date
	 * 
	 * @param lastRefreshed
	 *            as gregorian calendar
	 */
	public void setLastRefreshed(GregorianCalendar lastRefreshed) {
		this.lastRefreshed = lastRefreshed;
		UserManager.saveUserFile(this);
	}

	/**
	 * Touch files
	 */
	public void touch() {
		Calendar calendar = Calendar.getInstance();
		GregorianCalendar thisMonth = new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		if (lastRefreshed.before(thisMonth)) {
			// Update this file
			FileManager.updateUserFile(usernameHash);

			// Update key file
			FileManager.updateDataFile(getKeyFileLocation());

			// Update all data files
			for (String file : userFiles) {
				FileManager.updateDataFile(file);
			}
			setLastRefreshed(thisMonth);
		}

	}
}
