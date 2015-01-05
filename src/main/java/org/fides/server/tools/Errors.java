package org.fides.server.tools;

/**
 * Error messages that can be given to the user
 */
public class Errors {

	/**
	 * Unknown action
	 */
	public static final String UNKNOWN_ACTION = "Unknown action";

	/**
	 * Username already exists
	 */
	public static final String USNERNAME_EXISTS = "Username already exists";

	/**
	 * Cannot save userfile
	 */
	public static final String CANNOT_SAVE_USER_FILE = "Cannot save userfile";

	/**
	 * Username or password is empty
	 */
	public static final String USERNAME_OR_PASSWORD_EMPTY = "Username or password is empty";

	/**
	 * Username or password is incorrect
	 */
	public static final String USERNAME_OR_PASSWORD_INCORRECT = "Username or password is incorrect";

	/**
	 * Server cannot respond when user is locked
	 */
	public static final String SERVER_CANNOT_RESPOND = "The server cannot respond right now, please try again later";

	/**
	 * The user didn't add a file location to the request where one was needed.
	 */
	public static final String NO_FILE_LOCATION = "User didn't include a filelocation";

	/**
	 * The user tries to reach (upload or download to) a file that isn't his.
	 */
	public static final String FILE_WITHOUT_OWNERSHIP = "User doesn't own a file on the included location";

	/**
	 * File could not be found
	 */
	public static final String FILE_NOT_FOUND = "File could not be found on the server";

	/**
	 * File could not be removed
	 */
	public static final String FILE_NOT_REMOVED = "File could not be removed on the server";
}
