package org.fides.server.tools;

/**
 * Error messages that can be given to the user
 */
public class Errors {

	/**
	 * Unknown action
	 */
	public static final String UNKNOWNACTION = "Unknown action";

	/**
	 * Username already exists
	 */
	public static final String USNERNAMEEXISTS = "Username already exists";

	/**
	 * Cannot save userfile
	 */
	public static final String CANNOTSAVEUSERFILE = "Cannot save userfile";

	/**
	 * Username or password is empty
	 */
	public static final String USERNAMEORPASSWORDEMPTY = "Username or password is empty";

	/**
	 * Username or password is incorrect
	 */
	public static final String USERNAMEORPASSWORDINCORRECT = "Username or password is incorrect";

	/**
	 * The user didn't add a file location to the request where one was needed.
	 */
	public static final String NOFILELOCATION = "User didn't include a filelocation";

	/**
	 * The user tries to reach (upload or download to) a file that isn't his.
	 */
	public static final String FILEWITHOUTOWNERSHIP = "User doesn't own a file on the included location";

	/**
	 * File could not be found
	 */
	public static final String FILENOTFOUND = "File could not be found on the server";

	/**
	 * File could not be removed
	 */
	public static final String FILENOTREMOVED = "File could not be removed on the server";
}

