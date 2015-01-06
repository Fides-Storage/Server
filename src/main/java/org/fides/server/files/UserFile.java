package org.fides.server.files;

import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TreeSet;

import org.fides.server.tools.PropertiesManager;

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

	private Set<String> userFiles = new TreeSet<>();

	private String keyFile;

	private GregorianCalendar lastRefreshed;

	private long maxAmountOfUsedBytes;

	private long amountOfUsedBytes;

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
		this.maxAmountOfUsedBytes = PropertiesManager.getInstance().getMaxAmountOfBytesPerUser();
		this.amountOfUsedBytes = 0;
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

	public void setLastRefreshed(GregorianCalendar lastRefreshed) {
		this.lastRefreshed = lastRefreshed;
	}

	/**
	 * Change the max amount of used bytes
	 * 
	 * @param newMaxAmountOfUsedBytes
	 *            of bytes
	 * @return if succeeded
	 */
	public boolean changeMaxAmountOfUsedBytes(long newMaxAmountOfUsedBytes) {
		maxAmountOfUsedBytes = newMaxAmountOfUsedBytes;
		return UserManager.saveUserFile(this);
	}

	/**
	 * Getter for amount of free bytes
	 * 
	 * @return amount of free bytes
	 */
	public long getAmountOfFreeBytes() {
		if (maxAmountOfUsedBytes - amountOfUsedBytes <= 0) {
			return 0;
		}
		return maxAmountOfUsedBytes - amountOfUsedBytes;
	}

	/**
	 * Add amount of bytes to the used space
	 * 
	 * @param amountOfBytes
	 *            of used space
	 */
	public void addAmountOfBytes(long amountOfBytes) {
		amountOfUsedBytes += amountOfBytes;
		UserManager.saveUserFile(this);
	}

	/**
	 * Remove amount of bytes to the used space
	 * 
	 * @param amountOfBytes
	 *            of used space
	 */
	public void removeAmountOfBytes(long amountOfBytes) {
		amountOfUsedBytes -= amountOfBytes;
		UserManager.saveUserFile(this);
	}
}
