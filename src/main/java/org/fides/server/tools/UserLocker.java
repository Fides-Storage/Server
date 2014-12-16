package org.fides.server.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

/**
 * A static tool that can be used to lock a user on the server.
 */
public final class UserLocker {

	private static final FilenameFilter LOCKFILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".lock");
		}
	};

	private UserLocker() {
	}

	/**
	 * Tries to lock the user with the given username
	 * 
	 * @param usernameHash
	 *            The user that tries to be locked
	 * @return Whether the lock was possible and successful
	 */
	public static boolean lock(String usernameHash) {
		String userDir = PropertiesManager.getInstance().getUserDir();
		if (!StringUtils.isEmpty(userDir)) {
			try {
				File lockFile = new File(PropertiesManager.getInstance().getUserDir(), usernameHash.concat(".lock"));
				return lockFile.createNewFile();
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Removes the lock of the user with the given username
	 * 
	 * @param usernameHash
	 *            The user that can be unlocked
	 */
	public static void unlock(String usernameHash) {
		String userDir = PropertiesManager.getInstance().getUserDir();
		if (!StringUtils.isEmpty(userDir)) {
			File lockFile = new File(PropertiesManager.getInstance().getUserDir(), usernameHash.concat(".lock"));
			lockFile.delete();
		}
	}

	/**
	 * Clears all the available locks on the server. Should only be called on server startup.
	 */
	public static void clearAllLocks() {
		String userDir = PropertiesManager.getInstance().getUserDir();
		if (!StringUtils.isEmpty(userDir)) {
			File directory = new File(userDir);
			File[] lockFiles = directory.listFiles(LOCKFILTER);
			for (File file : lockFiles) {
				file.delete();
			}
		}
	}

	/**
	 * Checks whether the user with the given user name is locked or not
	 * 
	 * @param usernameHash
	 *            the user that could be locked
	 * @return true if the user is locked, false otherwise
	 */
	public static boolean isLocked(String usernameHash) {
		String userDir = PropertiesManager.getInstance().getUserDir();
		if (!StringUtils.isEmpty(userDir)) {
			File lockFile = new File(PropertiesManager.getInstance().getUserDir(), usernameHash.concat(".lock"));
			return lockFile.exists();
		}
		return false;
	}
}
